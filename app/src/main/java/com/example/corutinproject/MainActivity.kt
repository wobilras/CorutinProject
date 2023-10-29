package com.example.corutinproject

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var contactAdapter: ContactAdapter
    private lateinit var contactRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        contactRecyclerView = findViewById(R.id.contactRecyclerView)

        contactAdapter = ContactAdapter(emptyList())
        contactRecyclerView.layoutManager = LinearLayoutManager(this)
        contactRecyclerView.adapter = contactAdapter

        if (hasContactsPermission()) {
            loadContacts()
        } else {
            requestContactsPermission()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestContactsPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.READ_CONTACTS),
            PERMISSIONS_REQUEST_READ_CONTACTS
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadContacts() {
        launch {
            val contacts = withContext(Dispatchers.IO) {
                loadContactsInBackground()
            }
            contactAdapter.contacts = contacts
            contactAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("Range")
    private suspend fun loadContactsInBackground(): List<ContactModel> {
        val contacts = mutableListOf<ContactModel>()
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val contactId =
                    it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                val name =
                    it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val phoneNumber = getPhoneNumber(contactId)
                val contact = ContactModel(contactId, name, phoneNumber)
                contacts.add(contact)
            }
        }

        return contacts
    }

    @SuppressLint("Range")
    private fun getPhoneNumber(contactId: String): String {
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(contactId),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            }
        }

        return ""
    }

    companion object {
        private const val PERMISSIONS_REQUEST_READ_CONTACTS = 100
    }
}




