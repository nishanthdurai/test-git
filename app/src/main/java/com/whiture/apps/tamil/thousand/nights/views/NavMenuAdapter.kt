package com.whiture.apps.tamil.thousand.nights.views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.TextView
import com.whiture.apps.tamil.thousand.nights.R
import com.whiture.apps.tamil.thousand.nights.models.MetaBook
import com.whiture.apps.tamil.thousand.nights.objectArray
import com.whiture.apps.tamil.thousand.nights.stringArray
import org.json.JSONObject

/**
 * List adapter class used in book navigation menu
 */
class NavMenuAdapter(context: Context, private val book: MetaBook, var sectionId: Int,
                     var chapterId: Int): BaseExpandableListAdapter() {

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getGroup(groupPosition: Int): Any {
        return book.sections[groupPosition].title
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean {
        return true
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean,
                              convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.nav_menu_parent, null)
        view.findViewById<TextView>(R.id.navMenuParentText).text = book.sections[groupPosition].title
        return view
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return book.sections[groupPosition].chapters.size
    }

    override fun getChild(groupPosition: Int, childPosititon: Int): Any {
        return book.sections[groupPosition].chapters[childPosititon]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildView(groupPosition: Int, childPosititon: Int,
                              isLastChild: Boolean, convertView: View?,
                              parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.nav_menu_child, null)
        view.findViewById<TextView>(R.id.navMenuChildText).text =
            book.sections[groupPosition].chapters[childPosititon].title
        view.findViewById<ImageView>(R.id.bulletImg).visibility = if (sectionId - 1 == groupPosition
            && chapterId - 1 == childPosititon) View.VISIBLE else View.INVISIBLE
        return view
    }

    override fun getChildId(groupPosition: Int, childPosititon: Int): Long {
        return childPosititon.toLong()
    }

    override fun getGroupCount(): Int = book.sections.size

}

