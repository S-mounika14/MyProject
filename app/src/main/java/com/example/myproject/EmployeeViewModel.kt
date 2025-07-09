package com.example.myproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class EmployeeViewModel(private val repository: EmployeeRepository) : ViewModel() {


    private val _filteredEmployees = MutableLiveData<List<Employee>>()
    val filteredEmployees: LiveData<List<Employee>> = _filteredEmployees

    private val _invalidInput = MutableLiveData<String>()
    val invalidInput: LiveData<String> = _invalidInput

    private val _employees = MutableLiveData<List<Employee>>()
    val employees: LiveData<List<Employee>> = _employees

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error



    init {
        loadEmployees()
    }

    fun loadEmployees() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null;

                val result = repository.getEmployees()

                if (result.isSuccess) {
                    val employeeList = result.getOrNull() ?: emptyList()
                    _employees.value = employeeList
                    _filteredEmployees.value = employeeList
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Unknown error occurred"
                }
            } catch (e: Exception) {
                _error.value = "Error loading employees: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun searchEmployees(query: String) {
        val allEmployees = _employees.value ?: return
        val validationResult = validateAndSanitizeSearchQuery(query)
        if (validationResult.hasSpecialCharacters) {
            _invalidInput.value = "Please enter a valid name (alphanumeric and spaces only)"
            return
        }
        val sanitizedQuery = validationResult.sanitizedQuery
        if (sanitizedQuery.isNullOrEmpty()) {
            _filteredEmployees.value = allEmployees
            return
        }
        val filteredList = allEmployees.filter { employee ->
            employee.employee_name.contains(sanitizedQuery, ignoreCase = true) ||
                    employee.id.toString().contains(sanitizedQuery)
        }
        _filteredEmployees.value = filteredList
    }

    private data class ValidationResult(val sanitizedQuery: String?, val hasSpecialCharacters: Boolean)

    private fun validateAndSanitizeSearchQuery(query: String?): ValidationResult {
        if (query.isNullOrBlank()) {
            return ValidationResult(null, false)
        }
        val trimmedQuery = query.trim()
        val hasSpecialCharacters = trimmedQuery.contains(Regex("[^a-zA-Z0-9\\s]"))
        val sanitized = trimmedQuery.replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
        return ValidationResult(if (sanitized.isEmpty()) null else sanitized, hasSpecialCharacters)
    }
}