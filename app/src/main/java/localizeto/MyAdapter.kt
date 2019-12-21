package localizeto.androidexample

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import localizeto.localize

class MyAdapter(
    private val keys: Array<String>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class MyViewHolder(val topView: RelativeLayout) : RecyclerView.ViewHolder(topView)

    private var localizedKeys: MutableMap<String, String> = localizeKeys()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.my_item_view, parent, false) as RelativeLayout
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val key = keys[position]
        val value = localizedKeys[key]
        (holder as MyViewHolder).topView.children.forEach { v ->
            when (v.id) {
                R.id.item_key -> {
                    (v as TextView).text = key
                }
                R.id.item_value -> {
                    (v as TextView).text = value
                }
            }
        }
    }

    override fun getItemCount(): Int = keys.size

    private fun localizeKeys(): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        for (key in keys) {
            result[key] = key.localize()
        }
        return result
    }

    fun updateData() {
        this.localizedKeys = localizeKeys()
        notifyDataSetChanged()
    }
}