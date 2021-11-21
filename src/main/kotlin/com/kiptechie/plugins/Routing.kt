package com.kiptechie.plugins

import com.kiptechie.rooms.RoomController
import com.kiptechie.routes.chatSocket
import com.kiptechie.routes.getAllMessages
import io.ktor.application.*
import io.ktor.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val roomController by inject<RoomController>()
    install(Routing) {
        chatSocket(roomController)
        getAllMessages(roomController)
    }
}
