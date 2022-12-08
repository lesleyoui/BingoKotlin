package com.dev.bingokotin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dev.bingokotin.MainActivity.Companion.DATABASE_URL
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.*

class BingoActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        val TAG = BingoActivity::class.java.simpleName
        val STATUS_INIT = 0
        val STATUS_CREATED = 1
        val STATUS_JOINED = 2
        val STATUS_CREATOR_TURN = 3
        val STATUS_JOINED_TURN = 4
        val STATUS_CREATOR_BINGO = 5
        val STATUS_JOINED_BINGO = 6
    }

    lateinit var adapter: FirebaseRecyclerAdapter<Boolean, BingoActivity.NumberHolder>
    private lateinit var rvBingo: RecyclerView
    private lateinit var tvInfo: TextView
    private lateinit var numberMap: MutableMap<Int, Int>
    private var isCreator: Boolean = false
    lateinit var roomId: String
    private var isMyTure: Boolean = false
    set(value) {
        field = value // field就是this.isMyTurn
        tvInfo.text = (if (value) "請選號" else "等待對方選號")
    }
    val statusListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            var status : Long = snapshot.value as Long
            when(status.toInt()) {
                STATUS_CREATED -> {
                    tvInfo.text = "等待玩家加入"
                }
                STATUS_JOINED -> {
                    tvInfo.text = "玩家已加入"
                    FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
                        .child(roomId)
                        .child("status")
                        .setValue(STATUS_CREATOR_TURN)
                }
                STATUS_CREATOR_TURN -> {
                    isMyTure = isCreator
                }
                STATUS_JOINED_TURN -> {
                    isMyTure = !isCreator
                }
                STATUS_CREATOR_BINGO -> {
                    AlertDialog.Builder(this@BingoActivity)
                        .setTitle("Bingo")
                        .setMessage(if(isCreator) "恭喜你，賓果了" else "對方賓果了")
                        .setPositiveButton("OK"){dialog, which ->
                            endGame()
                        }
                        .show()
                }
                STATUS_JOINED_BINGO -> {
                    AlertDialog.Builder(this@BingoActivity)
                        .setTitle("Bingo")
                        .setMessage(if(!isCreator) "恭喜你，賓果了" else "對方賓果了")
                        .setPositiveButton("OK"){dialog, which ->
                            endGame()
                        }
                        .show()
                }
            }
        }

        override fun onCancelled(error: DatabaseError) {
        }
    }

    private fun endGame() {
        FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
            .child(roomId)
            .child("status")
            .removeEventListener(statusListener)
        if (isCreator){
            FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
                .child(roomId)
                .removeValue()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bingo)
        roomId = intent.getStringExtra("ROOM_ID").toString()
        isCreator = intent.getBooleanExtra("IS_CREATOR", false)
        Log.d(TAG, "roomId: $roomId")

        val buttons = mutableListOf<NumberButton>()

        if (isCreator) {
            // 創建database裡25個數字位置
            for (i in 1..25) {
                FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
                    .child(roomId)
                    .child("numbers")
                    .child(i.toString())
                    .setValue(false)
            }
            FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
                .child(roomId)
                .child("status")
                .setValue(STATUS_CREATED)
        } else {
            FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
                .child(roomId)
                .child("status")
                .setValue(STATUS_JOINED)
        }
        // 創建25ui元件 按鈕
        for (i in 0..24) {
            val button = NumberButton(this)
            button.number = i+1
            buttons.add(button)
        }
        // 打亂順序
        buttons.shuffle()
        numberMap = mutableMapOf()
        for (i in 0..24) {
            numberMap.put(buttons.get(i).number, i)
        }
        // recyclerView
        tvInfo = findViewById(R.id.tvInfo)
        rvBingo = findViewById(R.id.rvBingo)
        rvBingo.setHasFixedSize(true)
        rvBingo.layoutManager = GridLayoutManager(this, 5)
        // adapter
        val query = FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
            .child(roomId)
            .child("numbers")
            .orderByKey()
        val options = FirebaseRecyclerOptions.Builder<Boolean>()
            .setQuery(query, Boolean::class.java)
            .build()
        adapter = object : FirebaseRecyclerAdapter<Boolean, NumberHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberHolder {
                val view = layoutInflater.inflate(R.layout.item_bingo_button, parent, false)
                return NumberHolder(view)
            }

            override fun onBindViewHolder(holder: NumberHolder, position: Int, model: Boolean) {
                holder.button.setText(buttons.get(position).number.toString())
                holder.button.number = buttons.get(position).number
                holder.button.isEnabled = !buttons.get(position).picked
                holder.button.setOnClickListener(this@BingoActivity)
            }

            override fun onChildChanged(type: ChangeEventType, snapshot: DataSnapshot, newIndex: Int, oldIndex: Int) {
                super.onChildChanged(type, snapshot, newIndex, oldIndex)
                if(type == ChangeEventType.CHANGED) {
                    val number = snapshot.key?.toInt()
                    val pos = numberMap.get(number)
                    val picked = snapshot.value as Boolean
                    buttons.get(pos!!).picked = picked
                    val holder: NumberHolder = rvBingo.findViewHolderForAdapterPosition(pos!!) as NumberHolder
                    holder.button.isEnabled = false
                    // 檢查有無bingo
                    val nums = IntArray(25)
                    for (i in 0..25) {
                        nums[i] = if (buttons.get(i).picked) 1 else 0
                    }
                    var bingo = 0
                    for (i in 0..5) {
                        var sum = 0
                        for (j in 0..5) {
                            sum += nums[i*5 + j]
                        }
                        bingo += if (sum == 5) 1 else 0
                        sum = 0
                        for (j in 0..5) {
                            sum += nums[j*5 + i]
                        }
                        bingo += if (sum == 5) 1 else 0
                    }
                    Log.d(TAG, "Bingo: $bingo")
                    if (bingo > 0) {
                        FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
                            .child(roomId)
                            .child("status")
                            .setValue(if (isCreator) STATUS_CREATOR_BINGO else STATUS_JOINED_BINGO)
                    }
                }
            }
        }
        rvBingo.adapter = adapter


    }

    class NumberHolder(view: View) : RecyclerView.ViewHolder(view) {
        lateinit var button: NumberButton
        init {
            button = view.findViewById(R.id.button)
        }
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
        FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
            .child(roomId)
            .child("status")
            .addValueEventListener(statusListener)
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
        FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
            .child(roomId)
            .child("status")
            .removeEventListener(statusListener)
    }

    override fun onClick(v: View?) {
        if (isMyTure) {
            val number = (v as NumberButton).number
            FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
                .child(roomId)
                .child("numbers")
                .child(number.toString())
                .setValue(true)
            FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
                .child(roomId)
                .child("status")
                .setValue(if (isCreator) STATUS_JOINED_TURN else STATUS_CREATOR_TURN)
        }
    }
}