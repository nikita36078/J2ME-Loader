package ua.naiksoftware.j2meloader;

/**
 *
 * @author Naik
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class FileListAdapter extends BaseAdapter {

	private ArrayList<FSItem> list = new ArrayList<FSItem>();
	private final LayoutInflater li;

	public FileListAdapter(Context context, ArrayList<FSItem> arr) {
		if (arr != null) {
			list = arr;
		}
		li = LayoutInflater.from(context);
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
            view = li.inflate(R.layout.list_row, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.list_image);
            holder.label = (TextView) view.findViewById(R.id.list_header);
            holder.sublabel = (TextView) view.findViewById(R.id.list_subheader);
            view.setTag(holder);
        } else {
        	holder = (ViewHolder) view.getTag();
        }

        FSItem item = list.get(position);

        holder.icon.setImageResource(item.getImageId());
        holder.label.setText(item.getName());
        holder.sublabel.setText(item.getDescription());

        return view;
    }

	private static class ViewHolder {
		ImageView icon;
		TextView label;
		TextView sublabel;
	}
}
