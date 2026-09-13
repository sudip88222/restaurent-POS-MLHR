package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {

    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        subject: String = "Restaurant POS Report",
        textMessage: String = "",
        preferWhatsApp: Boolean = false
    ) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                if (textMessage.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, textMessage)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (preferWhatsApp) {
                    setPackage("com.whatsapp")
                }
            }

            try {
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                // If WhatsApp package was targeted but not found, fallback to standard chooser
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        if (textMessage.isNotBlank()) putExtra(Intent.EXTRA_TEXT, textMessage)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Share via"
                )
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareTextToWhatsApp(context: Context, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage("com.whatsapp")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback chooser
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                "Share via"
            )
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
}
