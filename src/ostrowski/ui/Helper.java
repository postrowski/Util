/*
 * Created on May 19, 2006
 *
 */

package ostrowski.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

public class Helper
{
   protected final List<Control> controlList = new ArrayList<>();

   public static Composite createComposite(Composite parent, int hSpan, int gridDataStyle)
   {
      return createComposite(parent, hSpan, gridDataStyle, 1/*columns*/);
   }

   public static Composite createComposite(Composite parent, int hSpan, int gridDataStyle, int columns)
   {
      Composite block = new Composite(parent, 0/*style*/);
      block.setLayout(new GridLayout(columns, false));
      GridData data = new GridData(gridDataStyle);
      data.horizontalSpan = hSpan;
      data.grabExcessVerticalSpace = true;
      block.setLayoutData(data);
      return block;
   }

   public Combo createCombo(Composite parent, int style, int hSpan, List<String> entries) {
      Combo combo = new Combo(parent, style);
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = hSpan;
      combo.setLayoutData(data);
      for (String name : entries) {
         combo.add(name);
      }
      if (combo.getChildren().length > 0) {
         combo.setText(combo.getItem(0));
      }
      controlList.add(combo);
      return combo;
   }

   public Label createLabel(Composite parent, String text, int hAlign, int hSpan, FontData fontData) {
      Label label = new Label(parent, SWT.NORMAL);
      if ((text != null) && (text.length() > 0)) {
         label.setText(text);
      }
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalAlignment = hAlign;
      data.horizontalSpan = hSpan;
      label.setLayoutData(data);
      if (fontData != null) {
         Font font = new Font(parent.getDisplay(), fontData);
         label.setFont(font);
         font.dispose();
      }
      controlList.add(label);
      return label;
   }

   public Text createText(Composite parent, String content, boolean editable, int hSpan) {
      return createText(parent, content, editable, hSpan, null);
   }

   public Text createText(Composite parent, String content, boolean editable, int hSpan, FontData fontData) {
      return createText(parent, content, editable, hSpan, fontData, 30/*minWidth*/);
   }

   public Text createText(Composite parent, String content, boolean editable, int hSpan, FontData fontData, int minWidth) {
      Text text = new Text(parent, (editable ? (SWT.LEFT | SWT.BORDER) : (SWT.CENTER | SWT.READ_ONLY | SWT.NO_FOCUS)));
      if (content != null) {
         text.setText(content);
      }
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = hSpan;
      data.minimumWidth = minWidth;
      data.widthHint = minWidth;
      text.setLayoutData(data);
      if (!editable) {
         Color bg = parent.getBackground();
         Color newBg = new Color(parent.getDisplay(), (128+ (bg.getRed()/2)), (128 + (bg.getGreen()/2)), (128 + (bg.getBlue()/2)));
         text.setBackground(newBg);
         newBg.dispose();
      }
      if (fontData != null) {
         Font font = new Font(parent.getDisplay(), fontData);
         text.setFont(font);
         font.dispose();
      }
      controlList.add(text);
      return text;
   }

   //  This won't work until spinner control supports negative values:
   public Spinner createSpinner(Composite parent, int min, int max, int value, int hSpan) {
       Spinner spinner = new Spinner(parent, (SWT.LEFT | SWT.BORDER));
       spinner.setMinimum(min);
       spinner.setMaximum(max);
       spinner.setSelection(value);
       spinner.setDigits(0);
       GridData data = new GridData(GridData.FILL_HORIZONTAL);
       data.horizontalSpan = hSpan;
       data.minimumWidth = 30;
       spinner.setLayoutData(data);
       return spinner;
   }

   public Group createGroup(Composite parent, String name, int columns, boolean sameSize, int hSpacing, int vSpacing) {
      return createGroup(parent, name, columns, sameSize, hSpacing, vSpacing, 1/*hSpan*/);
   }

   public Group createGroup(Composite parent, String name, int columns, boolean sameSize, int hSpacing, int vSpacing, int hSpan) {
      Group group = new Group(parent, SWT.SHADOW_NONE);
      GridLayout layout = new GridLayout(columns, sameSize);
      layout.horizontalSpacing = hSpacing;
      layout.verticalSpacing = vSpacing;
      group.setLayout(layout);

      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = hSpan;
      group.setLayoutData(data);

      group.setText(name);
      Font font = new Font(parent.getDisplay(), new FontData("Arial", 10, SWT.BOLD));
      group.setFont(font);
      font.dispose();
      controlList.add(group);
      return group;
   }

   public static org.eclipse.swt.widgets.List createList(Composite parent, int hSpan, FontData fontData) {
      org.eclipse.swt.widgets.List list = new org.eclipse.swt.widgets.List(parent, SWT.MULTI);
      if (fontData != null) {
         Font font = new Font(parent.getDisplay(), fontData);
         list.setFont(font);
         font.dispose();
      }
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = hSpan;
      list.setLayoutData(data);
      return list;
   }

   public Button createButton(Composite parent, String text, int hSpan, FontData fontData, SelectionListener sl) {
      Button button = new Button(parent, SWT.FLAT);
      if (text != null) {
         button.setText(text);
      }
      if (fontData != null) {
         Font font = new Font(parent.getDisplay(), fontData);
         button.setFont(font);
         font.dispose();
      }
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = hSpan;
      button.setLayoutData(data);
      button.addSelectionListener(sl);
      controlList.add(button);
      return button;
   }

   static public Button createRadioButton(Composite parent, String text, FontData fontData, SelectionListener sl) {
      Button button = new Button(parent, SWT.NORMAL | SWT.RADIO);
      if ((text != null) && (text.length() > 0)) {
         button.setText(text);
      }
      if (fontData != null) {
         Font font = new Font(parent.getDisplay(), fontData);
         button.setFont(font);
         font.dispose();
      }
      if (sl != null) {
         button.addSelectionListener(sl);
      }
      return button;
   }

   public byte getIntValue(Text text) {
      String level = text.getText();
      if ((level == null) || (level.length() == 0)) {
         level= "0";
      }
      try {
         return Byte.parseByte(level);
      }
      catch (Exception e) {
      }
      return 0;
   }

   public void enableControls(boolean enabledFlag) {
      for (Control element : controlList) {
         element.setEnabled(enabledFlag);
         if (element instanceof Text) {
            Text textElement = (Text) element;
            Color bg = element.getParent().getBackground();
            Color bgColor;
            if (enabledFlag) {
               bgColor = new Color(element.getParent().getDisplay(), 255, 255, 255);
            }
            else {
               bgColor = new Color(element.getParent().getDisplay(), (128+ (bg.getRed()/2)), (128 + (bg.getGreen()/2)), (128 + (bg.getBlue()/2)));
            }
            textElement.setBackground(bgColor);
            bgColor.dispose();
         }
      }
   }

}
