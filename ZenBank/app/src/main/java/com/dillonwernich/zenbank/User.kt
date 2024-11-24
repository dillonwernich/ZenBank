package com.dillonwernich.zenbank

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
@Entity(tableName = "user")
data class User(
    @PrimaryKey
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",

    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("language") @set:PropertyName("language")
    var language: String = "en",

    @get:PropertyName("debitAccountNumber") @set:PropertyName("debitAccountNumber")
    var debitAccountNumber: String = "",

    @get:PropertyName("debitBalance") @set:PropertyName("debitBalance")
    var debitBalance: Double = 0.0,

    @get:PropertyName("savingsAccountNumber") @set:PropertyName("savingsAccountNumber")
    var savingsAccountNumber: String = "",

    @get:PropertyName("savingsBalance") @set:PropertyName("savingsBalance")
    var savingsBalance: Double = 0.0,

    @get:PropertyName("profilePictureUrl") @set:PropertyName("profilePictureUrl")
    var profilePictureUrl: String? = null
)
