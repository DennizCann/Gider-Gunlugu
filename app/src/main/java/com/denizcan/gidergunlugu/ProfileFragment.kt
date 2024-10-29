package com.denizcan.gidergunlugu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.denizcan.gidergunlugu.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Kullanıcı bilgilerini yükle
        loadUserProfile()

        // Buton tıklanınca bilgileri kaydet veya güncelle
        binding.saveProfileButton.setOnClickListener {
            saveProfileToFirestore()
        }
    }

    // Kullanıcı bilgilerini yükleme
    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("userName")
                        val job = document.getString("job")
                        val dob = document.getString("dob")

                        // Bilgiler mevcutsa alanları doldur ve butonu 'Değiştir' olarak değiştir
                        binding.userNameInput.setText(userName)
                        binding.jobInput.setText(job)
                        binding.dobInput.setText(dob)
                        binding.saveProfileButton.text = "Değiştir"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Profil bilgisi alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Kullanıcı profilini Firestore'a kaydet veya güncelle
    private fun saveProfileToFirestore() {
        val userName = binding.userNameInput.text.toString()
        val job = binding.jobInput.text.toString()
        val dob = binding.dobInput.text.toString()

        if (userName.isNotEmpty() && job.isNotEmpty() && dob.isNotEmpty()) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val userId = currentUser.uid

                val profileData = hashMapOf(
                    "userName" to userName,
                    "job" to job,
                    "dob" to dob
                )

                firestore.collection("users").document(userId)
                    .set(profileData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Profil başarıyla kaydedildi", Toast.LENGTH_SHORT).show()
                        binding.saveProfileButton.text = "Değiştir" // Kaydetten sonra butonu değiştir
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Profil kaydedilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
