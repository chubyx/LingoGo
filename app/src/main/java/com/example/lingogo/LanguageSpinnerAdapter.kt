package com.example.lingogo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView


class LanguageSpinnerAdapter(context: Context, languages: List<LanguageOption>) :
    ArrayAdapter<LanguageOption>(context, 0, languages) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_spinner_language, parent, false)
        val language = getItem(position)

        val imgFlag = view.findViewById<ImageView>(R.id.imgFlag)
        val txtName = view.findViewById<TextView>(R.id.txtLangName)

        language?.let {
            imgFlag.setImageResource(it.flagResId)
            txtName.text = it.name
        }
        return view
    }
}