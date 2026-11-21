package com.example.hellodoc.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.hellodoc.R
import com.example.hellodoc.databinding.ActivityMainBinding

/**
 * TP5 : DrawerLayout + NavigationView + Navigation Component.
 * L'application ne contient que les destinations accessibles au patient.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val host = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = host.navController

        // Accueil, Médecins, Rendez-vous et Profil affichent le menu hamburger.
        // La fiche d'un médecin affiche une flèche de retour.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.doctorsFragment,
                R.id.appointmentsFragment,
                R.id.profileFragment
            ),
            binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navigationView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
