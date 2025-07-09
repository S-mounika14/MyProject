package com.example.myproject
import retrofit2.Response
import retrofit2.http.GET

interface EmployeeApiService {
    @GET("employees")
    suspend fun getEmployees(): Response<ApiResponse<List<Employee>>>
}
