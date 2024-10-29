package com.denizcan.gidergunlugu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.denizcan.gidergunlugu.databinding.FragmentAddIncomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddIncomeFragment : Fragment() {

    private var _binding: FragmentAddIncomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firestore ve Auth başlatma
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Mevcut gelir var mı kontrol et ve göster
        checkExistingIncome()

        binding.confirmButton.setOnClickListener {
            val income = binding.incomeInput.text.toString()

            if (income.isNotEmpty()) {
                // Gelir bilgisi güncelleniyor
                binding.displayIncome.text = "Gelir: $income"
                binding.displayIncome.visibility = View.VISIBLE

                // Firestore'a gelir miktarını kaydet veya güncelle
                saveOrUpdateIncomeToFirestore(income)

            } else {
                Toast.makeText(requireContext(), "Lütfen bir gelir miktarı girin", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Mevcut gelir kontrolü
    private fun checkExistingIncome() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Kullanıcının mevcut gelir verisini kontrol et
            firestore.collection("users").document(userId)
                .collection("incomes").get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        // İlk gelir belgesini al ve ekranda göster
                        val incomeDoc = documents.documents[0]
                        val existingIncome = incomeDoc.getString("income")

                        binding.incomeInput.setText(existingIncome)
                        binding.displayIncome.text = "Gelir: $existingIncome"
                        binding.displayIncome.visibility = View.VISIBLE
                        binding.confirmButton.text = "Değiştir" // Buton metnini değiştir
                    } else {
                        // Mevcut gelir yoksa onayla butonu görünsün
                        binding.confirmButton.text = "Onayla"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gelir alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Gelir verisini Firestore'a kaydet veya güncelle
    private fun saveOrUpdateIncomeToFirestore(income: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Mevcut gelir belgesi var mı kontrol et
            firestore.collection("users").document(userId)
                .collection("incomes").get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        // Var olan gelir belgesini güncelle
                        val incomeDocId = documents.documents[0].id
                        firestore.collection("users").document(userId)
                            .collection("incomes").document(incomeDocId)
                            .update("income", income)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Gelir başarıyla güncellendi", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Gelir güncellenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // Yeni gelir belgesi ekle
                        val incomeData = hashMapOf(
                            "income" to income,
                            "timestamp" to System.currentTimeMillis()
                        )

                        firestore.collection("users").document(userId)
                            .collection("incomes").add(incomeData)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Gelir başarıyla kaydedildi", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Gelir kaydedilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gelir alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
