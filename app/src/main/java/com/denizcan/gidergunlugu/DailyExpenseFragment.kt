package com.denizcan.gidergunlugu

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.denizcan.gidergunlugu.databinding.FragmentDailyExpenseBinding

class DailyExpenseFragment : Fragment() {

    private var _binding: FragmentDailyExpenseBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var expenseAdapter: ArrayAdapter<String>
    private val expenseList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDailyExpenseBinding.inflate(inflater, container, false)
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

        // Gider ekle butonu
        binding.addExpenseButton.setOnClickListener {
            showAddExpenseDialog()
        }

        // Gider çıkar butonu
        binding.removeExpenseButton.setOnClickListener {
            showRemoveExpenseDialog()
        }
    }

    // Gider ekleme dialogu
    // Gider ekleme dialogu
    // Gider ekleme dialogu
    private fun showAddExpenseDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_daily_expense, null)
        val expenseNameInput = dialogView.findViewById<EditText>(R.id.expenseNameInput)
        val expenseAmountInput = dialogView.findViewById<EditText>(R.id.expenseAmountInput)
        val expenseDayInput = dialogView.findViewById<EditText>(R.id.expenseDayInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Günlük Gider Ekle")
            .setView(dialogView)
            .setPositiveButton("Ekle") { dialog, _ ->
                val name = expenseNameInput.text.toString()
                val amount = expenseAmountInput.text.toString().toDoubleOrNull()
                val days = expenseDayInput.text.toString().toIntOrNull()

                if (name.isNotEmpty() && amount != null && days != null) {
                    val totalExpense = amount * days
                    val expense = "$name - $totalExpense TL" // Parantez içinde çarpma işlemi gösterilmeyecek
                    expenseList.add(expense)
                    expenseAdapter.notifyDataSetChanged()

                    // Gideri Firestore'a kaydet
                    saveDailyExpenseToFirestore(name, totalExpense.toString())
                } else {
                    Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
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
                removeDailyExpenseFromFirestore(selectedExpense)
            }
            .show()
    }

    // Firestore'a günlük gider ekleme
    // Firestore'a günlük harcama ekleme
    private fun saveDailyExpenseToFirestore(name: String, totalAmount: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            val expenseData = hashMapOf(
                "name" to name,
                "amount" to totalAmount,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("users").document(userId)
                .collection("expenses") // Günlük harcamalar normal giderler gibi kaydediliyor
                .add(expenseData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Günlük gider başarıyla kaydedildi", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Günlük gider kaydedilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    // Firestore'dan günlük gider çıkarma
    private fun removeDailyExpenseFromFirestore(expense: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            firestore.collection("users").document(userId)
                .collection("dailyExpenses")
                .whereEqualTo("name", expense.split(" - ")[0])
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        firestore.collection("users").document(userId)
                            .collection("dailyExpenses").document(document.id).delete()
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
