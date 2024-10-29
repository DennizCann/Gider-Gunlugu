package com.denizcan.gidergunlugu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.denizcan.gidergunlugu.databinding.FragmentAddCurrencyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddCurrencyFragment : Fragment() {

    private var _binding: FragmentAddCurrencyBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCurrencyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firestore ve Auth başlatma
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Para birimi kaydet butonu
        binding.saveCurrencyButton.setOnClickListener {
            val selectedCurrency = when (binding.currencyRadioGroup.checkedRadioButtonId) {
                R.id.radioDollar -> "Dolar"
                R.id.radioEuro -> "Euro"
                R.id.radioPound -> "Sterlin"
                R.id.radioTL -> "TL"
                else -> null
            }

            if (selectedCurrency != null) {
                saveCurrencyToFirestore(selectedCurrency)
            } else {
                Toast.makeText(requireContext(), "Lütfen bir para birimi seçin", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Firestore'a para birimi kaydet
    private fun saveCurrencyToFirestore(currency: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            val currencyData = hashMapOf(
                "currency" to currency,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("users").document(userId)
                .collection("settings").document("currency")
                .set(currencyData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Para birimi başarıyla kaydedildi: $currency", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Para birimi kaydedilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
