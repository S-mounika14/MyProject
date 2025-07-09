package com.example.myproject

data class ApiResponse<T>(
    val status: String,
    val data: T?,
    val message: String?
)