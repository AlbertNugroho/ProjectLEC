import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lecproject.GameDetail
import com.example.lecproject.ThumbnailItem
import com.example.lecproject.databinding.FragmentDetailGameScreenBinding
import com.example.lecproject.databinding.ItemThumbnailBinding

class ThumbnailAdapter(
    private val items: List<ThumbnailItem>,
    public val onItemClick: (ThumbnailItem) -> Unit
) : RecyclerView.Adapter<ThumbnailAdapter.ViewHolder>() {

    private val sortedItems = items.sortedByDescending { it.isVideo }

    inner class ViewHolder(private val binding: ItemThumbnailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ThumbnailItem) {
            Glide.with(binding.imageThumbnail.context)
                .load(item.url)
                .into(binding.imageThumbnail)

            binding.videoIcon.visibility = if (item.isVideo) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemThumbnailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = sortedItems.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sortedItems[position])
    }
}
