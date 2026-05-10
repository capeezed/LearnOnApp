package com.learnon.app.instructor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.learnon.app.instructor.data.remote.InstructorNetwork
import com.learnon.app.instructor.data.remote.InstructorTokenStore
import com.learnon.app.instructor.data.repository.InstructorRepositoryImpl
import com.learnon.app.instructor.navigation.InstructorRoot
import com.learnon.app.instructor.ui.theme.LearnOnInstructorTheme
import com.learnon.app.instructor.viewmodel.InstructorViewModel
import com.learnon.app.instructor.viewmodel.InstructorViewModelFactory

class InstructorDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val api = InstructorNetwork.createApi(this)
        val tokenStore = InstructorTokenStore(this)
        val repository = InstructorRepositoryImpl(this, api)
        val factory = InstructorViewModelFactory(repository, tokenStore)

        setContent {
            LearnOnInstructorTheme {
                val viewModel: InstructorViewModel = viewModel(factory = factory)
                InstructorRoot(viewModel = viewModel)
            }
        }
    }
}
