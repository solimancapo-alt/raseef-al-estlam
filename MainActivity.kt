
package com.soliman.counter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

data class Product(var name: String, var factor: Double, var boxes: Double = 0.0)

class MainActivity : Activity() {

    private val products = mutableListOf<Product>()
    private lateinit var list: LinearLayout
    private lateinit var totals: TextView
    private var tallyOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(s: String, size: Float = 16f): TextView =
        TextView(this).apply {
            text = s
            textSize = size
            gravity = Gravity.CENTER
            setTextColor(0xFF38291F.toInt())
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

    private fun button(s: String, action: () -> Unit): Button =
        Button(this).apply {
            text = s
            setOnClickListener { action() }
        }

    private fun field(hintText: String, value: String = ""): EditText =
        EditText(this).apply {
            hint = hintText
            setText(value)
            textSize = 16f
            singleLine = true
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

    private fun buildScreen() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(0xFFF7F3EE.toInt())
        }

        outer.addView(label("رصيف الاستلام", 25f))
        outer.addView(label("سجّل عدد الصناديق وسيتم حساب الكمية تلقائيًا", 13f))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        actions.addView(
            button("إضافة صنف", { editProduct(null) }),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        actions.addView(
            button(if (tallyOnly) "عرض الكل" else "إظهار التقفيل", {
                tallyOnly = !tallyOnly
                refresh()
            }),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        actions.addView(
            button("PDF", { exportPdf() }),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        outer.addView(actions)

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            addView(list)
        }

        outer.addView(label("الصنف                         الصناديق                 الكمية", 13f))
        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        totals = label("")
        outer.addView(totals)

        outer.addView(
            button("مسح أعداد التقفيل", {
                AlertDialog.Builder(this)
                    .setTitle("مسح التقفيل")
                    .setMessage("هل تريد مسح جميع أعداد الصناديق؟")
                    .setPositiveButton("مسح") { _, _ ->
                        products.forEach { it.boxes = 0.0 }
                        refresh()
                    }
                    .setNegativeButton("إلغاء", null)
                    .show()
            })
        )

        setContentView(outer)
        refresh()
    }

    private fun refresh() {
        list.removeAllViews()

        products.forEachIndexed { index, p ->

            if (tallyOnly && p.boxes == 0.0) return@forEachIndexed

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(3), 0, dp(3))
            }

            val name = TextView(this).apply {
                text = p.name
                textSize = 14f
                setTextColor(0xFF38291F.toInt())
                gravity = Gravity.CENTER_VERTICAL
            }

            row.addView(
                name,
                LinearLayout.LayoutParams(0, dp(54), 1.35f)
            )

            val boxes = field("0", if (p.boxes == 0.0) "" else fmt(p.boxes)).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                gravity = Gravity.CENTER

                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        p.boxes = text.toString().toDoubleOrNull() ?: 0.0
                        refresh()
                    }
                }
            }

            row.addView(
                boxes,
                LinearLayout.LayoutParams(0, dp(50), 0.8f)
            )

            row.addView(
                label(fmt(p.boxes * p.factor)),
                LinearLayout.LayoutParams(0, dp(50), 0.8f)
            )

            row.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle(p.name)
                    .setItems(arrayOf("تعديل الصنف", "حذف الصنف")) { _, which ->
                        if (which == 0) {
                            editProduct(index)
                        } else {
                            AlertDialog.Builder(this)
                                .setMessage("هل تريد حذف الصنف؟")
                                .setPositiveButton("حذف") { _, _ ->
                                    products.removeAt(index)
                                    refresh()
                                }
                                .setNegativeButton("إلغاء", null)
                                .show()
                        }
                    }
                    .show()
                true
            }

            list.addView(row)

            val divider = View(this).apply {
                setBackgroundColor(0xFFD8C9BA.toInt())
            }

            list.addView(
                divider,
                LinearLayout.LayoutParams(-1, dp(1))
            )
        }

        val boxTotal = products.sumOf { it.boxes }
        val qtyTotal = products.sumOf { it.boxes * it.factor }

        totals.text =
            "إجمالي الصناديق: ${fmt(boxTotal)}     إجمالي الكميات: ${fmt(qtyTotal)}"
    }

    private fun editProduct(index: Int?) {
        val old = index?.let { products[it] }

        val name = field("اسم الصنف", old?.name ?: "")
        val factor = field("معامل الصندوق", old?.factor?.let { fmt(it) } ?: "")

        factor.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            addView(name)
            addView(factor)
        }

        AlertDialog.Builder(this)
            .setTitle(if (old == null) "إضافة صنف" else "تعديل صنف")
            .setView(panel)
            .setPositiveButton("حفظ") { _, _ ->

                val n = name.text.toString().trim()
                val f = factor.text.toString().toDoubleOrNull()

                if (n.isNotEmpty() && f != null && f > 0) {

                    if (old == null) {
                        products.add(Product(n, f))
                    } else {
                        old.name = n
                        old.factor = f
                    }

                    refresh()

                } else {
                    Toast.makeText(
                        this,
                        "اكتب اسمًا ومعاملًا صحيحًا",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun fmt(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString()
        else String.format("%.2f", v)

    private val CREATE_PDF = 4102
    private var pendingPdf: ByteArray? = null

    private fun exportPdf() {

        val selected = products.filter { it.boxes != 0.0 }

        if (selected.isEmpty()) {
            Toast.makeText(
                this,
                "لا توجد أصناف مسجلة في التقفيل",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val doc = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("رصيف الاستلام", 555f, 48f, paint)

        paint.textSize = 14f

        canvas.drawText("الصنف", 555f, 95f, paint)
        canvas.drawText("عدد الصناديق", 310f, 95f, paint)
        canvas.drawText("الكمية", 140f, 95f, paint)

        var y = 130f

        selected.forEach { p ->
            canvas.drawText(p.name.take(32), 555f, y, paint)
            canvas.drawText(fmt(p.boxes), 310f, y, paint)
            canvas.drawText(fmt(p.boxes * p.factor), 140f, y, paint)
            y += 28f
        }

        canvas.drawText(
            "إجمالي الصناديق: ${fmt(selected.sumOf { it.boxes })}",
            555f,
            y + 30f,
            paint
        )

        canvas.drawText(
            "إجمالي الكميات: ${fmt(selected.sumOf { it.boxes * it.factor })}",
            555f,
            y + 58f,
            paint
        )

        doc.finishPage(page)

        try {
            val output = java.io.ByteArrayOutputStream()
            doc.writeTo(output)
            pendingPdf = output.toByteArray()

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
                putExtra(Intent.EXTRA_TITLE, "تقفيل-رصيف-الاستلام.pdf")
            }

            startActivityForResult(intent, CREATE_PDF)

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "تعذر تجهيز ملف PDF",
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            doc.close()
        }
    }

    @Deprecated("Android document picker callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CREATE_PDF && resultCode == RESULT_OK) {

            val uri: Uri? = data?.data
            val bytes = pendingPdf

            if (uri != null && bytes != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use {
                        it.write(bytes)
                    }

                    Toast.makeText(
                        this,
                        "تم حفظ ملف PDF بنجاح",
                        Toast.LENGTH_LONG
                    ).show()

                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "تعذر حفظ ملف PDF",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        pendingPdf = null
    }
}
