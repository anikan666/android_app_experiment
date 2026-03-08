package com.sarangi.app.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (viewModel.hasExistingProfile()) {
            onOnboardingComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Progress indicator
        if (state.currentStep > 0) {
            LinearProgressIndicator(
                progress = { (state.currentStep + 1).toFloat() / state.totalSteps },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        AnimatedContent(
            targetState = state.currentStep,
            transitionSpec = {
                slideInHorizontally { if (targetState > initialState) it else -it } + fadeIn() togetherWith
                    slideOutHorizontally { if (targetState > initialState) -it else it } + fadeOut()
            },
            label = "onboarding_step"
        ) { step ->
            when (step) {
                0 -> WelcomeStep(onNext = { viewModel.nextStep() })
                1 -> AboutYouStep(state, viewModel)
                2 -> PracticeGoalsStep(state, viewModel)
                3 -> BodyPreferencesStep(state, viewModel)
                4 -> TransparencyStep(onNext = { viewModel.nextStep() }, onBack = { viewModel.previousStep() })
                5 -> CalibrationStep(state, viewModel)
                6 -> ReadyStep(state, viewModel, onOnboardingComplete)
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Sarangi",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Your daily practice partner",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "I'm an AI practice companion. I help structure your daily violin practice, track your progress, and make your time with your human teacher more productive.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutYouStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    val experiences = listOf(
        "Never played",
        "Played a little (< 3 months)",
        "Some experience (3-12 months)",
        "Returning after a break"
    )
    val frequencies = listOf("Weekly", "Biweekly", "Monthly")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("About You", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("Your name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(20.dp))

        Text("What's your experience with violin?", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        experiences.forEach { exp ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = state.experience == exp,
                    onClick = { viewModel.updateExperience(exp) },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.tertiary)
                )
                Text(exp, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Do you currently have a violin teacher?")
            Switch(
                checked = state.hasTeacher,
                onCheckedChange = { viewModel.updateHasTeacher(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.tertiary)
            )
        }

        if (state.hasTeacher) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("How often do you see them?", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                frequencies.forEach { freq ->
                    FilterChip(
                        selected = state.teacherFrequency == freq.lowercase(),
                        onClick = { viewModel.updateTeacherFrequency(freq.lowercase()) },
                        label = { Text(freq) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        NavigationButtons(
            onBack = { viewModel.previousStep() },
            onNext = { viewModel.nextStep() },
            nextEnabled = state.name.isNotBlank()
        )
    }
}

@Composable
private fun PracticeGoalsStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    val durations = listOf(15, 20, 30, 45)
    val times = listOf("Morning", "Afternoon", "Evening", "Varies")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Practice Goals", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Text("How many days per week can you realistically practise?")
        Spacer(modifier = Modifier.height(8.dp))
        Text("${state.practiceGoalDays} days", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Slider(
            value = state.practiceGoalDays.toFloat(),
            onValueChange = { viewModel.updatePracticeGoalDays(it.toInt()) },
            valueRange = 2f..7f,
            steps = 4,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.tertiary,
                activeTrackColor = MaterialTheme.colorScheme.tertiary
            )
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text("How long is a typical practice session for you?")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            durations.forEach { dur ->
                FilterChip(
                    selected = state.sessionDurationMinutes == dur,
                    onClick = { viewModel.updateSessionDuration(dur) },
                    label = { Text("$dur min") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("What time of day do you usually practise?")
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            times.forEach { time ->
                FilterChip(
                    selected = state.practiceTime == time,
                    onClick = { viewModel.updatePracticeTime(time) },
                    label = { Text(time) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        NavigationButtons(onBack = { viewModel.previousStep() }, onNext = { viewModel.nextStep() })
    }
}

@Composable
private fun BodyPreferencesStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    val handSizes = listOf(
        "Small" to "Smaller spans, may need adapted fingerings",
        "Medium" to "Standard fingering recommendations",
        "Large" to "Extended reaches are comfortable"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Your Body & Preferences", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Hand size (affects fingering recommendations)")
        Spacer(modifier = Modifier.height(8.dp))
        handSizes.forEach { (size, desc) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.handSize == size)
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surface
                ),
                onClick = { viewModel.updateHandSize(size) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.handSize == size,
                        onClick = { viewModel.updateHandSize(size) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.tertiary)
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(size, fontWeight = FontWeight.Medium)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = state.physicalConstraints,
            onValueChange = { viewModel.updatePhysicalConstraints(it) },
            label = { Text("Any physical constraints I should know about?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("Optional") }
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = state.musicalBackground,
            onValueChange = { viewModel.updateMusicalBackground(it) },
            label = { Text("Do you have any musical background?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("e.g. 'I sang in a choir' or 'I play guitar'") }
        )

        Spacer(modifier = Modifier.weight(1f))
        NavigationButtons(onBack = { viewModel.previousStep() }, onNext = { viewModel.nextStep() })
    }
}

@Composable
private fun TransparencyStep(onNext: () -> Unit, onBack: () -> Unit) {
    val canDo = listOf(
        Icons.Default.Mic to "I CAN hear your pitch and rhythm through your phone mic",
        Icons.Default.EventNote to "I CAN design practice sessions tailored to you",
        Icons.Default.QuestionAnswer to "I CAN answer any question about violin, music theory, or technique"
    )
    val cantDo = listOf(
        Icons.Default.Visibility to "I CANNOT see your finger placement or bow hold precisely",
        Icons.Default.Person to "I CANNOT replace your human teacher for physical technique",
        Icons.Default.Psychology to "I WILL ask you questions to diagnose what I can't directly detect"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("How I Work", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        canDo.forEach { (icon, text) ->
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                Text(text, modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        cantDo.forEach { (icon, text) ->
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                Text(text, modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "I'll always be honest about what I'm confident in and what I'm guessing at.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.weight(1f))
        NavigationButtons(onBack = onBack, onNext = onNext)
    }
}

@Composable
private fun CalibrationStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Environment Calibration", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        if (!state.isCalibrating && !state.calibrationComplete) {
            Text(
                "Let's calibrate your room. Set your phone where you'll normally place it during practice, and stay quiet for 10 seconds.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { viewModel.startCalibration() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Calibration")
            }
        } else if (state.isCalibrating) {
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                color = MaterialTheme.colorScheme.tertiary,
                strokeWidth = 6.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Listening...", style = MaterialTheme.typography.titleLarge)
            Text("Please stay quiet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        } else if (state.calibrationComplete) {
            val noiseDesc = when (state.noiseProfile?.noiseLevel) {
                com.sarangi.core.model.NoiseLevel.LOW -> "low" to "I can provide full feedback including pitch, rhythm, and tone analysis."
                com.sarangi.core.model.NoiseLevel.MODERATE -> "moderate" to "Pitch feedback may be less reliable, but rhythm and tempo tracking will work well."
                com.sarangi.core.model.NoiseLevel.HIGH -> "high" to "I'll focus on rhythm and tempo. For pitch feedback, try a quieter space."
                null -> "unknown" to ""
            }
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your room has ${noiseDesc.first} background noise.", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(noiseDesc.second, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.weight(1f))
        NavigationButtons(
            onBack = { viewModel.previousStep() },
            onNext = { viewModel.nextStep() },
            nextEnabled = state.calibrationComplete
        )
    }
}

@Composable
private fun ReadyStep(state: OnboardingState, viewModel: OnboardingViewModel, onComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "You're all set, ${state.name.ifBlank { "there" }}.",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Let's start your first practice session.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = { viewModel.saveProfile(onComplete) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(16.dp),
            enabled = !state.isSaving
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onTertiary)
            } else {
                Text("Start Practising", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun NavigationButtons(
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextEnabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onBack) {
            Text("Back")
        }
        Button(
            onClick = onNext,
            enabled = nextEnabled,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Next")
        }
    }
}
