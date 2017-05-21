package ua.naiksoftware.j2meloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 *
 * @author Naik
 */
public class AppsListAdapter extends BaseAdapter {

	private List<AppItem> list;
	private final LayoutInflater layoutInflater;
    private Context context;

	public AppsListAdapter(Context context, List<AppItem> list) {
		if (list != null) {
			this.list = list;
		}
		this.layoutInflater = LayoutInflater.from(context);
        this.context = context;
	}

	public int getCount() {
		return list.size();
	}

	public Object getItem(int position) {
		return list.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View view, ViewGroup viewGroup) {
    	ViewHolder holder;
        if (view == null) {
            view = layoutInflater.inflate(R.layout.list_row_jar, null);
            holder = new  ViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.list_image);
            holder.name = (TextView) view.findViewById(R.id.list_title);
			holder.author = (TextView) view.findViewById(R.id.list_author);
			holder.version = (TextView) view.findViewById(R.id.list_version);
            view.setTag(holder);
        } else {
        	holder = (ViewHolder) view.getTag();
        }
        AppItem item = list.get(position);

        holder.icon.setImageDrawable(new BitmapDrawable(context.getResources(), item.getImagePath()));
        holder.name.setText(item.getTitle());
		holder.author.setText(item.getAuthor());
		holder.version.setText(item.getVersion());
        
        return view;
    }

	private static class ViewHolder {
		ImageView icon;
		TextView name;
		TextView author;
		TextView version;
	}
}
