package com.devil7softwares.aescamera.list

import java.io.Serializable

data class ContactItem (val name: String, val contactNo: String, val photoUri: String?, val previouslySelected: Boolean) : Serializable