/*
 * Copyright 2012 Kulikov Dmitriy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.lcdui.pointer;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * Привязка прямоугольников.
 * 
 * <pre>
 * Суть:
 * 
 * SNAP_LEFT
 * 
 *      +------+------+
 *      | B           |
 *  +---+             |
 *  | A |             |
 *  +---+             |
 *      |             |
 *      +------+------+
 *      
 * MID_LEFT
 * 
 *      +------+------+
 *      | B           |
 *    +---+           |
 *    | A |           |
 *    +---+           |
 *      |             |
 *      +------+------+
 *      
 * ALIGN_LEFT
 * 
 *      +------+------+
 *      | B           |
 *      +---+         |
 *      | A |         |
 *      +---+         |
 *      |             |
 *      +------+------+
 *      
 * LEFT_HCENTER
 * 
 *      +------+------+
 *      | B           |
 *      |  +---+      |
 *      |  | A |      |
 *      |  +---+      |
 *      |             |
 *      +------+------+
 *      
 * ALIGN_HCENTER
 * 
 *      +------+------+
 *      | B           |
 *      |    +---+    |
 *      |    | A |    |
 *      |    +---+    |
 *      |             |
 *      +------+------+
 *      
 * RIGHT_HCENTER
 * 
 *      +------+------+
 *      | B           |
 *      |      +---+  |
 *      |      | A |  |
 *      |      +---+  |
 *      |             |
 *      +------+------+
 *      
 * ALIGN_RIGHT
 * 
 *      +------+------+
 *      | B           |
 *      |         +---+
 *      |         | A |
 *      |         +---+
 *      |             |
 *      +------+------+
 *      
 * MID_RIGHT
 * 
 *      +------+------+
 *      | B           |
 *      |           +---+
 *      |           | A |
 *      |           +---+
 *      |             |
 *      +------+------+
 *      
 * SNAP_RIGHT
 * 
 *      +------+------+
 *      | B           |
 *      |             +---+
 *      |             | A |
 *      |             +---+
 *      |             |
 *      +------+------+
 *      
 * </pre>
 * 
 * Для удобства здесь определены два набора констант по сторонам света.
 * Константы на EXT_ определяют привязку снаружи другого прямоугольника,
 * константы на INT_ определяют привязку внутри другого прямоугольника.
 */
public class RectSnap
{
	public static final int NO_SNAP = 0;
	
	public static final int HORIZONTAL_MASK = 0x0000FFFF;
	public static final int VERTICAL_MASK   = 0xFFFF0000;
	
	public static final int COARSE_HORIZONTAL_MASK = 0x0000001F;
	public static final int COARSE_VERTICAL_MASK   = 0x001F0000;
	
	public static final int FINE_MASK = HORIZONTAL_MASK | VERTICAL_MASK;
	public static final int COARSE_MASK = COARSE_HORIZONTAL_MASK | COARSE_VERTICAL_MASK;
	
	public static final int SNAP_LEFT		= 0x00000001;
	public static final int ALIGN_LEFT		= 0x00000002;
	public static final int ALIGN_HCENTER	= 0x00000004;
	public static final int ALIGN_RIGHT		= 0x00000008;
	public static final int SNAP_RIGHT		= 0x00000010;
	public static final int MID_LEFT		= 0x00000020;
	public static final int MID_RIGHT		= 0x00000040;
	public static final int LEFT_HCENTER	= 0x00000080;
	public static final int RIGHT_HCENTER	= 0x00000100;
	
	public static final int SNAP_TOP		= 0x00010000;
	public static final int ALIGN_TOP		= 0x00020000;
	public static final int ALIGN_VCENTER	= 0x00040000;
	public static final int ALIGN_BOTTOM	= 0x00080000;
	public static final int SNAP_BOTTOM		= 0x00100000;
	public static final int MID_TOP			= 0x00200000;
	public static final int MID_BOTTOM		= 0x00400000;
	public static final int TOP_VCENTER		= 0x00800000;
	public static final int BOTTOM_VCENTER	= 0x01000000;
	
	public static final int EXT_NORTHWEST	= SNAP_TOP		| SNAP_LEFT;
	public static final int EXT_NORTH		= SNAP_TOP		| ALIGN_HCENTER;
	public static final int EXT_NORTHEAST	= SNAP_TOP		| SNAP_RIGHT;
	public static final int EXT_EAST		= ALIGN_VCENTER	| SNAP_RIGHT;
	public static final int EXT_SOUTHEAST	= SNAP_BOTTOM	| SNAP_RIGHT;
	public static final int EXT_SOUTH		= SNAP_BOTTOM	| ALIGN_HCENTER;
	public static final int EXT_SOUTHWEST	= SNAP_BOTTOM	| SNAP_LEFT;
	public static final int EXT_WEST		= ALIGN_VCENTER | SNAP_LEFT;
	
	public static final int INT_NORTHWEST	= ALIGN_TOP		| ALIGN_LEFT;
	public static final int INT_NORTH		= ALIGN_TOP		| ALIGN_HCENTER;
	public static final int INT_NORTHEAST	= ALIGN_TOP		| ALIGN_RIGHT;
	public static final int INT_EAST		= ALIGN_VCENTER	| ALIGN_RIGHT;
	public static final int INT_SOUTHEAST	= ALIGN_BOTTOM	| ALIGN_RIGHT;
	public static final int INT_SOUTH		= ALIGN_BOTTOM	| ALIGN_HCENTER;
	public static final int INT_SOUTHWEST	= ALIGN_BOTTOM	| ALIGN_LEFT;
	public static final int INT_WEST		= ALIGN_VCENTER | ALIGN_LEFT;
	
	/**
	 * Определить режим привязки двух прямоугольников (если есть).
	 * Версия для Rect.
	 * 
	 * @param target что привязывать
	 * @param origin куда привязывать
	 * @param radius на каком расстоянии начинает действовать привязка
	 * @param mask маска разрешенных привязок
	 * @param biaxial если true, то привязка будет либо по обоим осям, либо ее вообще не будет
	 * @return режим привязки, или NO_SNAP
	 */
	public static int getSnap(Rect target, Rect origin, int radius, int mask, boolean biaxial)
	{
		return getSnap(new RectF(target), new RectF(origin), radius, mask, biaxial);
	}
	
	/**
	 * Определить режим привязки двух прямоугольников (если есть).
	 * Версия для RectF.
	 * 
	 * @param target что привязывать
	 * @param origin куда привязывать
	 * @param radius на каком расстоянии начинает действовать привязка
	 * @param mask маска разрешенных привязок
	 * @param biaxial если true, то привязка будет либо по обоим осям, либо ее вообще не будет
	 * @return режим привязки, или NO_SNAP
	 */
	public static int getSnap(RectF target, RectF origin, float radius, int mask, boolean biaxial)
	{
		int snap = NO_SNAP;
		
		if(((mask & SNAP_LEFT) != 0) && Math.abs(origin.left - target.right) <= radius)
		{
			snap |= SNAP_LEFT;
		}
		else if(((mask & ALIGN_LEFT) != 0) && Math.abs(origin.left - target.left) <= radius)
		{
			snap |= ALIGN_LEFT;
		}
		else if(((mask & ALIGN_HCENTER) != 0) && Math.abs(origin.centerX() - target.centerX()) <= radius)
		{
			snap |= ALIGN_HCENTER;
		}
		else if(((mask & ALIGN_RIGHT) != 0) && Math.abs(origin.right - target.right) <= radius)
		{
			snap |= ALIGN_RIGHT;
		}
		else if(((mask & SNAP_RIGHT) != 0) && Math.abs(origin.right - target.left) <= radius)
		{
			snap |= SNAP_RIGHT;
		}
		else if(((mask & MID_LEFT) != 0) && Math.abs(origin.left - target.centerX()) <= radius)
		{
			snap |= MID_LEFT;
		}
		else if(((mask & MID_RIGHT) != 0) && Math.abs(origin.right - target.centerX()) <= radius)
		{
			snap |= MID_RIGHT;
		}
		else if(((mask & LEFT_HCENTER) != 0) && Math.abs(origin.centerX() - target.right) <= radius)
		{
			snap |= LEFT_HCENTER;
		}
		else if(((mask & RIGHT_HCENTER) != 0) && Math.abs(origin.centerX() - target.left) <= radius)
		{
			snap |= RIGHT_HCENTER;
		}
		
		if(((mask & SNAP_TOP) != 0) && Math.abs(origin.top - target.bottom) <= radius)
		{
			snap |= SNAP_TOP;
		}
		else if(((mask & ALIGN_TOP) != 0) && Math.abs(origin.top - target.top) <= radius)
		{
			snap |= ALIGN_TOP;
		}
		else if(((mask & ALIGN_VCENTER) != 0) && Math.abs(origin.centerY() - target.centerY()) <= radius)
		{
			snap |= ALIGN_VCENTER;
		}
		else if(((mask & ALIGN_BOTTOM) != 0) && Math.abs(origin.bottom - target.bottom) <= radius)
		{
			snap |= ALIGN_BOTTOM;
		}
		else if(((mask & SNAP_BOTTOM) != 0) && Math.abs(origin.bottom - target.top) <= radius)
		{
			snap |= SNAP_BOTTOM;
		}
		else if(((mask & MID_TOP) != 0) && Math.abs(origin.top - target.centerY()) <= radius)
		{
			snap |= MID_TOP;
		}
		else if(((mask & MID_BOTTOM) != 0) && Math.abs(origin.bottom - target.centerY()) <= radius)
		{
			snap |= MID_BOTTOM;
		}
		else if(((mask & TOP_VCENTER) != 0) && Math.abs(origin.centerY() - target.bottom) <= radius)
		{
			snap |= TOP_VCENTER;
		}
		else if(((mask & BOTTOM_VCENTER) != 0) && Math.abs(origin.centerY() - target.top) <= radius)
		{
			snap |= BOTTOM_VCENTER;
		}
		
		if(biaxial && (((snap & HORIZONTAL_MASK) == 0) ^ ((snap & VERTICAL_MASK) == 0)))
		{
			snap = NO_SNAP;
		}
		
		return snap;
	}
	
	/**
	 * Определить наилучшую привязку двух прямоугольников (существует всегда),
	 * и опционально вернуть смещение от этой привязки.
	 * Версия для Rect и Point.
	 * 
	 * @param target что привязывать
	 * @param origin куда привязывать
	 * @param offset точка, в которую будет записано смещение от привязки (или null)
	 * @return режим привязки
	 */
	public static int getSnap(Rect target, Rect origin, Point offset)
	{
		PointF point = new PointF();
		int snap = getSnap(new RectF(target), new RectF(origin), point);
		
		offset.set(Math.round(point.x), Math.round(point.y));
		
		return snap;
	}
	
	/**
	 * Определить наилучшую привязку двух прямоугольников (существует всегда),
	 * и опционально вернуть смещение от этой привязки.
	 * Версия для RectF и PointF.
	 * 
	 * @param target что привязывать
	 * @param origin куда привязывать
	 * @param offset точка, в которую будет записано смещение от привязки (или null)
	 * @return режим привязки
	 */
	public static int getSnap(RectF target, RectF origin, PointF offset)
	{
		float[] rx =
		{
			origin.left - target.right,
			origin.left - target.left,
			origin.centerX() - target.centerX(),
			origin.right - target.right,
			origin.right - target.left,
			origin.left - target.centerX(),
			origin.right - target.centerX(),
			origin.centerX() - target.right,
			origin.centerX() - target.left
		};
		
		float[] ry =
		{
			origin.top - target.bottom,
			origin.top - target.top,
			origin.centerY() - target.centerY(),
			origin.bottom - target.bottom,
			origin.bottom - target.top,
			origin.top - target.centerY(),
			origin.bottom - target.centerY(),
			origin.centerY() - target.bottom,
			origin.centerY() - target.top
		};
			
		int[] snpx =
		{
			SNAP_LEFT,
			ALIGN_LEFT,
			ALIGN_HCENTER,
			ALIGN_RIGHT,
			SNAP_RIGHT,
			MID_LEFT,
			MID_RIGHT,
			LEFT_HCENTER,
			RIGHT_HCENTER
		};
		
		int[] snpy =
		{
			SNAP_TOP,
			ALIGN_TOP,
			ALIGN_VCENTER,
			ALIGN_BOTTOM,
			SNAP_BOTTOM,
			MID_TOP,
			MID_BOTTOM,
			TOP_VCENTER,
			BOTTOM_VCENTER
		};
		
		int minIX = 0;
		int minIY = 0;
		
		for(int index = 1; index < snpx.length; index++)
		{
			if(Math.abs(rx[index]) < Math.abs(rx[minIX]))
			{
				minIX = index;
			}
			
			if(Math.abs(ry[index]) < Math.abs(ry[minIY]))
			{
				minIY = index;
			}
		}
		
		if(offset != null)
		{
			offset.x = -rx[minIX];
			offset.y = -ry[minIY];
		}
		
		return snpx[minIX] | snpy[minIY];
	}
	
	/**
	 * Привязать один прямоугольник к другому,
	 * опционально сместив его на некоторое расстояние от привязки.
	 * Версия для Rect и Point.
	 * 
	 * @param target что привязывать
	 * @param origin куда привязывать
	 * @param mode как привязывать
	 * @param offset точка, в которой содержится смещение от привязки (или null)
	 */
	public static void snap(Rect target, Rect origin, int mode, Point offset)
	{
		RectF a = new RectF(target);
		RectF b = new RectF(origin);
		
		PointF point = null;
		
		if(offset != null)
		{
			point = new PointF(offset);
		}
		
		snap(a, b, mode, point);
		
		target.set(Math.round(a.left), Math.round(a.top), Math.round(a.right), Math.round(a.bottom));
	}
	
	/**
	 * Привязать один прямоугольник к другому,
	 * опционально сместив его на некоторое расстояние от привязки.
	 * Версия для RectF и PointF.
	 * 
	 * @param target что привязывать
	 * @param origin куда привязывать
	 * @param mode как привязывать
	 * @param offset точка, в которой содержится смещение от привязки (или null)
	 */
	public static void snap(RectF target, RectF origin, int mode, PointF offset)
	{
		float width = target.width();
		float height = target.height();
		
		switch(mode & HORIZONTAL_MASK)
		{
			case SNAP_LEFT:
				target.right = origin.left;
				target.left = target.right - width;
				break;
				
			case ALIGN_LEFT:
				target.left = origin.left;
				target.right = target.left + width;
				break;
				
			case ALIGN_HCENTER:
				target.left = origin.left + (origin.width() - width) / 2;
				target.right = target.left + width;
				break;
				
			case ALIGN_RIGHT:
				target.right = origin.right;
				target.left = target.right - width;
				break;
				
			case SNAP_RIGHT:
				target.left = origin.right;
				target.right = target.left + width;
				break;
				
			case MID_LEFT:
				target.left = origin.left - width / 2;
				target.right = target.left + width;
				break;
				
			case MID_RIGHT:
				target.left = origin.right - width / 2;
				target.right = target.left + width;
				break;
				
			case LEFT_HCENTER:
				target.right = origin.centerX();
				target.left = target.right - width;
				break;
				
			case RIGHT_HCENTER:
				target.left = origin.centerX();
				target.right = target.left + width;
				break;
		}
		
		switch(mode & VERTICAL_MASK)
		{
			case SNAP_TOP:
				target.bottom = origin.top;
				target.top = target.bottom - height;
				break;
				
			case ALIGN_TOP:
				target.top = origin.top;
				target.bottom = target.top + height;
				break;
				
			case ALIGN_VCENTER:
				target.top = origin.top + (origin.height() - height) / 2;
				target.bottom = target.top + height;
				break;
				
			case ALIGN_BOTTOM:
				target.bottom = origin.bottom;
				target.top = target.bottom - height;
				break;
				
			case SNAP_BOTTOM:
				target.top = origin.bottom;
				target.bottom = target.top + height;
				break;
				
			case MID_TOP:
				target.top = origin.top - height / 2;
				target.bottom = target.top + height;
				break;
				
			case MID_BOTTOM:
				target.top = origin.bottom - height / 2;
				target.bottom = target.top + height;
				break;
				
			case TOP_VCENTER:
				target.bottom = origin.centerY();
				target.top = target.bottom - height;
				break;
				
			case BOTTOM_VCENTER:
				target.top = origin.centerY();
				target.bottom = target.top + height;
				break;
		}
		
		if(offset != null)
		{
			target.offset(offset.x, offset.y);
		}
	}
	
	public static String toString(int mode)
	{
		String res = "[";
		
		switch(mode & HORIZONTAL_MASK)
		{
			case SNAP_LEFT:
				res += "SNAP_LEFT";
				break;
				
			case ALIGN_LEFT:
				res += "ALIGN_LEFT";
				break;
				
			case ALIGN_HCENTER:
				res += "ALIGN_HCENTER";
				break;
				
			case ALIGN_RIGHT:
				res += "ALIGN_RIGHT";
				break;
				
			case SNAP_RIGHT:
				res += "SNAP_RIGHT";
				break;
				
			case MID_LEFT:
				res += "MID_LEFT";
				break;
				
			case MID_RIGHT:
				res += "MID_RIGHT";
				break;
				
			case LEFT_HCENTER:
				res += "LEFT_HCENTER";
				break;
				
			case RIGHT_HCENTER:
				res += "RIGHT_HCENTER";
				break;
				
			default:
				res += mode & HORIZONTAL_MASK;
		}
		
		res += "|";
		
		switch(mode & VERTICAL_MASK)
		{
			case SNAP_TOP:
				res += "SNAP_TOP";
				break;
				
			case ALIGN_TOP:
				res += "ALIGN_TOP";
				break;
				
			case ALIGN_VCENTER:
				res += "ALIGN_VCENTER";
				break;
				
			case ALIGN_BOTTOM:
				res += "ALIGN_BOTTOM";
				break;
				
			case SNAP_BOTTOM:
				res += "SNAP_BOTTOM";
				break;
				
			case MID_TOP:
				res += "MID_TOP";
				break;
				
			case MID_BOTTOM:
				res += "MID_BOTTOM";
				break;
				
			case TOP_VCENTER:
				res += "TOP_VCENTER";
				break;
				
			case BOTTOM_VCENTER:
				res += "BOTTOM_VCENTER";
				break;
				
			default:
				res += mode & VERTICAL_MASK;
		}
		
		res += "]";
		
		return res;
	}
}