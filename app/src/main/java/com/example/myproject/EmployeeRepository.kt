package com.example.myproject
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class EmployeeRepository(private val context: Context) {

    private val apiService = RetrofitClient.getEmployeeApiService()

    suspend fun getEmployees(): Result<List<Employee>> {
        return withContext(Dispatchers.IO) {
            try {
                // Check network connectivity
                if (!isNetworkAvailable()) {
                    return@withContext Result.failure(
                        Exception("No internet connection. Please check your network.")
                    )
                }

                val response: Response<ApiResponse<List<Employee>>> = apiService.getEmployees()

                when {
                    response.isSuccessful -> {
                        val apiResponse = response.body()
                        if (apiResponse?.status == "success" && apiResponse.data != null) {
                            if (apiResponse.data.isEmpty()) {
                                Result.failure(Exception("No employees found"))
                            } else {
                                Result.success(apiResponse.data)
                            }
                        } else {
                            Result.failure(Exception("API returned unsuccessful status: ${apiResponse?.message}"))
                        }
                    }
                    response.code() == 404 -> {
                        Result.failure(Exception("Employees not found (404)"))
                    }
                    response.code() == 429 -> {
                        val retryAfter = response.headers().get("Retry-After")?.toIntOrNull()?.let { seconds ->
                            "Please wait $seconds seconds before trying again."
                        } ?: "Please try again later."
                        Result.failure(Exception("Too many requests (429). $retryAfter"))
                    }
                    response.code() >= 500 -> {
                        Result.failure(Exception("Server error (${response.code()}). Please try again later."))
                    }
                    else -> {
                        Result.failure(Exception("Request failed with code: ${response.code()}"))
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                Result.failure(Exception("Request timeout. Please try again."))
            } catch (e: java.net.UnknownHostException) {
                Result.failure(Exception("Cannot reach server. Please check your connection."))
            } catch (e: Exception) {
                Result.failure(Exception("Unexpected error: ${e.message}"))
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}
