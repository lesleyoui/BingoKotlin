package com.dev.bingokotin

class Room (var id:String,
            var title:String,
            var status: Int,
            var init: Member?,
            var join: Member?) {
    constructor() : this("", "", 0, null, null)
    constructor(title: String, init: Member?) : this("", title, 0, init, null )
    constructor(id: String, title: String, init: Member?) : this(id, title, 0, init, null)
}