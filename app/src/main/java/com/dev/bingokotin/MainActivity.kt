package com.dev.bingokotin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.EmailBuilder
import com.firebase.ui.auth.AuthUI.IdpConfig.GoogleBuilder
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import java.util.*

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener, View.OnClickListener {
    companion object { // 靜態寫法 用object
        val TAG = MainActivity::class.java.simpleName
        val RC_SIGN_IN = 100
        val DATABASE_URL = "https://bingo-c9ac5-default-rtdb.asia-southeast1.firebasedatabase.app"
    }

    private val avatarIds = intArrayOf(
        R.drawable.avatar01,
        R.drawable.avatar02,
        R.drawable.avatar03,
        R.drawable.avatar04,
        R.drawable.avatar05,
        R.drawable.avatar06,
        R.drawable.avatar07,
        R.drawable.avatar08
    )
    private var member: Member? = null
    private lateinit var adapter: FirebaseRecyclerAdapter<Room, MainActivity.RoomHolder>
    private lateinit var tvNickname: TextView
    private lateinit var groupAvatars: Group
    private lateinit var imgAvatar: ImageView
    private lateinit var avatar1: ImageView
    private lateinit var avatar2: ImageView
    private lateinit var avatar3: ImageView
    private lateinit var avatar4: ImageView
    private lateinit var avatar5: ImageView
    private lateinit var avatar6: ImageView
    private lateinit var avatar7: ImageView
    private lateinit var avatar8: ImageView
    private lateinit var fab: FloatingActionButton
    private lateinit var rvRooms: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViews();
        tvNickname.setOnClickListener{
            FirebaseAuth.getInstance().currentUser?.let {
                showNicknameDialog(it.uid, tvNickname.text.toString())
            }
        }
        imgAvatar.setOnClickListener {
            groupAvatars.visibility =
                if (groupAvatars.visibility == View.GONE) View.VISIBLE else View.GONE
        }
        avatar1.setOnClickListener(this)
        avatar2.setOnClickListener(this)
        avatar3.setOnClickListener(this)
        avatar4.setOnClickListener(this)
        avatar5.setOnClickListener(this)
        avatar6.setOnClickListener(this)
        avatar7.setOnClickListener(this)
        avatar8.setOnClickListener(this)
        fab.setOnClickListener {
            val etRoomName = EditText(this)
            etRoomName.setText("Welcome")
            AlertDialog.Builder(this)
                .setTitle("Room Name")
                .setMessage("Please enter your room name")
                .setView(etRoomName)
                .setPositiveButton("OK") {dialog,  which ->
                    var room = Room(etRoomName.text.toString(), member)
                    FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
                        .push().setValue(room, object : DatabaseReference.CompletionListener {
                            override fun onComplete(error: DatabaseError?, ref: DatabaseReference) {
                                val roomId = ref.key
                                FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
                                    .child(roomId.toString())
                                    .child("id")
                                    .setValue(roomId)

                                val bingo = Intent(this@MainActivity, BingoActivity::class.java)
                                bingo.putExtra("ROOM_ID", roomId)
                                bingo.putExtra("IS_CREATOR", true)
                                startActivity(bingo)
                            }

                        })
                }.setNegativeButton("Cancel", null)
                .show()
        }
        // RecyclerView
        rvRooms.setHasFixedSize(true)
        rvRooms.layoutManager = LinearLayoutManager(this)
        val query = FirebaseDatabase.getInstance(DATABASE_URL).getReference("rooms")
            .limitToLast(30)
        val options = FirebaseRecyclerOptions.Builder<Room>()
            .setQuery(query, Room::class.java)
            .build()
        adapter = object : FirebaseRecyclerAdapter<Room, RoomHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomHolder {
                val view : View = layoutInflater.inflate(R.layout.item_room, parent, false)
                return RoomHolder(view)
            }

            override fun onBindViewHolder(holder: RoomHolder, position: Int, model: Room) {
                holder.title.text = model.title
                holder.image.setImageResource(avatarIds[model.init!!.avatarId])
                holder.itemView.setOnClickListener{
                    val intent = Intent(this@MainActivity, BingoActivity::class.java)
                    intent.putExtra("ROOM_ID", model.id)
                    startActivity(intent)
                }
            }

        }
        rvRooms.adapter = adapter

    }

    class RoomHolder(view: View): RecyclerView.ViewHolder(view) {
        var image = view.findViewById<ImageView>(R.id.imgHead)
        var title = view.findViewById<TextView>(R.id.tvRoomName)
    }

    private fun findViews() {
        tvNickname = findViewById(R.id.nickname)
        groupAvatars = findViewById(R.id.groupAvatars)
        groupAvatars.visibility = View.GONE
        imgAvatar = findViewById(R.id.imgUser)
        avatar1 = findViewById(R.id.avatar1)
        avatar2 = findViewById(R.id.avatar2)
        avatar3 = findViewById(R.id.avatar3)
        avatar4 = findViewById(R.id.avatar4)
        avatar5 = findViewById(R.id.avatar5)
        avatar6 = findViewById(R.id.avatar6)
        avatar7 = findViewById(R.id.avatar7)
        avatar8 = findViewById(R.id.avatar8)
        fab = findViewById(R.id.fab)
        rvRooms = findViewById(R.id.rvRooms)
    }

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(this)
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(this)
        adapter.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_action_signout -> {
                FirebaseAuth.getInstance().signOut()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        auth.currentUser?.also {
            Log.d(TAG, it.uid)
            FirebaseDatabase.getInstance(DATABASE_URL).getReference("users")
                .child(it.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        member = snapshot.getValue(Member::class.java)
                        // 暱稱
                        member?.nickname?.also { nick ->
                            tvNickname.text = nick
                        } ?: showNicknameDialog(it)

                        // 頭像
                        member?.let {
                            imgAvatar.setImageResource(avatarIds[it.avatarId])
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
            it.displayName?.run {
                FirebaseDatabase.getInstance(DATABASE_URL)
                    .getReference("users")
                    .child(it.uid)
                    .child("displayName")
                    .setValue(this)
                    .addOnCompleteListener{ Log.d(TAG, "done") }
            }
            /*FirebaseDatabase.getInstance(URL)
                .getReference("users")
                .child(it.uid)
                .child("nickname")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.value?.also { nick ->
                            Log.d(TAG, "nickname: $it")
                        } ?: showNicknameDialog(it)
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })*/
        } ?: signUp() // ?: 如果是null 則執行後面的

    }
    private fun showNicknameDialog(uid: String, nick:String?) {
        val etNickname = EditText(this)
        etNickname.setText(nick)
        AlertDialog.Builder(this)
            .setTitle("Nickname")
            .setMessage("Please enter your nickname")
            .setView(etNickname)
            .setPositiveButton("OK") {dialog,  which ->
                FirebaseDatabase.getInstance(DATABASE_URL).getReference("users")
                    .child(uid)
                    .child("nickname")
                    .setValue(etNickname.text.toString())
            }.show()
    }
    private fun showNicknameDialog(user: FirebaseUser) {
        val nick = user.displayName
        val uid = user.uid
        showNicknameDialog(uid, nick)

    }

    private fun signUp() {
        startActivityForResult( // 使用Firebase UI 登入
            AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(
                    Arrays.asList(
                        EmailBuilder().build(),  // 使用email登入
                        GoogleBuilder().build() // 使用Google登入 可以一直往下加登入方式
                    )
                )
                .setIsSmartLockEnabled(false) // SmartLock 幫使用者記住登入資料 但開發時先使用false 以利不同帳號測試 上線時再註解掉
                .build(), RC_SIGN_IN
        )
    }

    override fun onClick(v: View?) {
        val selectedId = when(v!!.id) {
            R.id.avatar1 -> 0
            R.id.avatar2 -> 1
            R.id.avatar3 -> 2
            R.id.avatar4 -> 3
            R.id.avatar5 -> 4
            R.id.avatar6 -> 5
            R.id.avatar7 -> 6
            R.id.avatar8 -> 7
            else -> 0
        }
        FirebaseDatabase.getInstance(DATABASE_URL).getReference("users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child("avatarId")
            .setValue(selectedId)
        groupAvatars.visibility = View.GONE
    }
}