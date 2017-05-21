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

package javax.microedition.lcdui;

import android.graphics.RectF;

public interface Overlay
{
	/**
	 * Нацелить оверлей на указанный Canvas
	 * @param canvas Canvas, которому при необходимости следует передавать
	 * 				 нажатия клавиш и движения указателя
	 */
	public void setTarget(Canvas canvas);
	
	/**
	 * Вызывается при изменении размера реального (например, при повороте)
	 * или виртуального экранов.
	 * 
	 * @param screen размер реального экрана устройства
	 * @param virtualScreen размер виртуального экрана, который доступен мидлету
	 */
	public void resize(RectF screen, RectF virtualScreen);
	
	/**
	 * Вызывается при перерисовке экрана.
	 * См. метод paint() в Canvas.
	 * 
	 * @param g Graphics, через который следует вести рисование
	 */
	public void paint(Graphics g);
	
	/**
	 * Вызывается при первом нажатии аппаратной клавиши.
	 * 
	 * @param keyCode код нажатой клавиши
	 * @return true, если нажатие обработано здесь и дальше его передавать не нужно
	 */
	public boolean keyPressed(int keyCode);
	
	/**
	 * Вызывается при повторном (2, 3, и т.д.) нажатии аппаратной клавиши.
	 * 
	 * @param keyCode код нажатой клавиши
	 * @return true, если нажатие обработано здесь и дальше его передавать не нужно
	 */
	public boolean keyRepeated(int keyCode);
	
	/**
	 * Вызывается при отпускании аппаратной клавиши.
	 * 
	 * @param keyCode код нажатой клавиши
	 * @return true, если нажатие обработано здесь и дальше его передавать не нужно
	 */
	public boolean keyReleased(int keyCode);
	
	/**
	 * Вызывается при касании экрана указателем.
	 * 
	 * @param pointer индекс указателя (всегда 0, если указатель один;
	 * 									может быть больше 0, если устройство поддерживает мультитач)
	 * @param x горизонтальная координата точки касания указателя на экране
	 * @param y вертикальная координата точки касания указателя на экране
	 * @return true, если касание обработано здесь и дальше его передавать не нужно
	 */
	public boolean pointerPressed(int pointer, float x, float y);
	
	/**
	 * Вызывается при перемещении указателя по экрану.
	 * 
	 * @param pointer индекс указателя (всегда 0, если указатель один;
	 * 									может быть больше 0, если устройство поддерживает мультитач)
	 * @param x горизонтальная координата точки касания указателя на экране
	 * @param y вертикальная координата точки касания указателя на экране
	 * @return true, если движение обработано здесь и дальше его передавать не нужно
	 */
	public boolean pointerDragged(int pointer, float x, float y);
	
	/**
	 * Вызывается при отпускании указателя.
	 * 
	 * @param pointer индекс указателя (всегда 0, если указатель один;
	 * 									может быть больше 0, если устройство поддерживает мультитач)
	 * @param x горизонтальная координата точки касания указателя на экране
	 * @param y вертикальная координата точки касания указателя на экране
	 * @return true, если касание обработано здесь и дальше его передавать не нужно
	 */
	public boolean pointerReleased(int pointer, float x, float y);
	
	/**
	 * Показать оверлей.
	 * Вызывается Canvas'ом при первом касании экрана указателем.
	 */
	public void show();
	
	/**
	 * Скрыть оверлей.
	 * Вызывается Canvas'ом при отпускании с экрана последнего указателя.
	 */
	public void hide();
}