package com.example.lecproject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Register : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameInput = findViewById<EditText>(R.id.username_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val registerButton = findViewById<Button>(R.id.register_button)

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val users = getUserList()

            if (users.any { it.username == username }) {
                Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            users.add(User(username, password))
            saveUserList(users)

            Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    private fun getUserList(): MutableList<User> {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(USERS_KEY, null)
        val type = object : TypeToken<MutableList<User>>() {}.type
        return if (json != null) {
            Gson().fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    private fun saveUserList(users: List<User>) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = Gson().toJson(users)
        editor.putString(USERS_KEY, json)
        editor.apply()
    }
}
