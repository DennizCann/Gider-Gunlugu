package com.denizcan.gidergunlugu

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.denizcan.gidergunlugu.databinding.FragmentAddExpenseBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var expenseAdapter: ArrayAdapter<String>
    private val expenseList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firestore ve Auth başlatma
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // ListView için adapter
        expenseAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, expenseList)
        binding.expenseListView.adapter = expenseAdapter

        // Mevcut giderleri yükle
        loadExpensesFromFirestore()

        // Gider ekle butonu
        binding.addExpenseButton.setOnClickListener {
            showAddExpenseDialog()
        }

        // Gider çıkar butonu
        binding.removeExpenseButton.setOnClickListener {
            showRemoveExpenseDialog()
        }
    }

    // Mevcut giderleri Firestore'dan oku ve listeye ekle
    private fun loadExpensesFromFirestore() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            firestore.collection("users").document(userId)
                .collection("expenses")
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val name = document.getString("name")
                        val amount = document.getString("amount")
                        if (name != null && amount != null) {
                            val expense = "$name - $amount"
                            expenseList.add(expense)
                        }
                    }

                    // Giderler listesi boş değilse "Henüz gider eklenmedi" mesajını gizle
                    if (expenseList.isNotEmpty()) {
                        binding.noExpensesText.visibility = View.GONE
                    }

                    // Listeyi güncelle
                    expenseAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Giderler alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Gider ekleme dialogu
    private fun showAddExpenseDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null)
        val expenseNameInput = dialogView.findViewById<EditText>(R.id.expenseNameInput)
        val expenseAmountInput = dialogView.findViewById<EditText>(R.id.expenseAmountInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Gider Ekle")
            .setView(dialogView)
            .setPositiveButton("Ekle") { dialog, _ ->
                val name = expenseNameInput.text.toString()
                val amount = expenseAmountInput.text.toString()
                if (name.isNotEmpty() && amount.isNotEmpty()) {
                    val expense = "$name - $amount"
                    expenseList.add(expense)
                    expenseAdapter.notifyDataSetChanged()

                    // Gideri Firestore'a kaydet
                    saveExpenseToFirestore(name, amount)

                    if (binding.noExpensesText.visibility == View.VISIBLE) {
                        binding.noExpensesText.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(requireContext(), "Lütfen gider adı ve miktarını girin", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // Gider çıkarma dialogu
    private fun showRemoveExpenseDialog() {
        if (expenseList.isEmpty()) {
            Toast.makeText(requireContext(), "Çıkarılacak gider yok", Toast.LENGTH_SHORT).show()
            return
        }

        val expenseNames = expenseList.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Gider Çıkart")
            .setItems(expenseNames) { _, which ->
                val selectedExpense = expenseList[which]
                expenseList.removeAt(which)
                expenseAdapter.notifyDataSetChanged()

                // Gideri Firestore'dan çıkar
                removeExpenseFromFirestore(selectedExpense)

                if (expenseList.isEmpty()) {
                    binding.noExpensesText.visibility = View.VISIBLE
                }
            }
            .show()
    }

    // Firestore'a gider ekleme
    private fun saveExpenseToFirestore(name: String, amount: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            val expenseData = hashMapOf(
                "name" to name,
                "amount" to amount,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("users").document(userId)
                .collection("expenses").add(expenseData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Gider başarıyla kaydedildi", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gider kaydedilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Firestore'dan gider çıkarma
    private fun removeExpenseFromFirestore(expense: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            firestore.collection("users").document(userId)
                .collection("expenses")
                .whereEqualTo("name", expense.split(" - ")[0])
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        firestore.collection("users").document(userId)
                            .collection("expenses").document(document.id).delete()
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Gider başarıyla silindi", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gider silinemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
