package ostrowski.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ostrowski.DebugBreak;

public abstract class SerializableObject
{
   static final String  _charEncoding          = "ISO-8859-1"; // "ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1";

   public abstract void serializeToStream(DataOutputStream out);

   public abstract void serializeFromStream(DataInputStream in);

//   static public SerializableObject createNewInstanceOfClass(Class< ? extends SerializableObject> type) {
//      try {
//         Constructor< ? extends SerializableObject> constructors;
//         constructors = type.getConstructor(new Class[] {});
//         return constructors.newInstance(new Object[] {});
//      } catch (NoSuchMethodException e1) {
//         e1.printStackTrace();
//      } catch (SecurityException e1) {
//         e1.printStackTrace();
//      } catch (InstantiationException e) {
//         e.printStackTrace();
//      } catch (IllegalAccessException e) {
//         e.printStackTrace();
//      } catch (IllegalArgumentException e) {
//         e.printStackTrace();
//      } catch (InvocationTargetException e) {
//         e.printStackTrace();
//      }
//       return null;
//   }

   // Unknown object serialization
   public static void writeObject(Object obj, DataOutputStream out) throws IOException {
      if (obj instanceof String) {
         writeToStream((String)obj, out);
      }
      else if (obj instanceof Integer) {
         writeToStream((Integer)obj, out);
      }
      else if (obj instanceof Byte) {
         writeToStream((Byte)obj, out);
      }
      else if (obj instanceof Boolean) {
         writeToStream(((Boolean)obj).booleanValue(), out);
      }
      else if (obj instanceof SerializableObject) {
         ((SerializableObject)obj).serializeToStream(out);
      }
      else {
         String message = "object of type " +obj.getClass().getName() +" cant be serialized.";
         DebugBreak.debugBreak(message);
         throw new IllegalArgumentException(message);
      }
   }

   // String serialization
   public static void writeToStream(String data, DataOutputStream out) throws IOException {
      if ((data == null) || (data.length() == 0)) {
         out.writeShort(0);
      } else {
         byte[] strout = data.getBytes(_charEncoding);
         out.writeShort(strout.length);
         out.write(strout);
      }
   }

   public static String readString(DataInputStream in) throws IOException {
      short strlen = in.readShort();
      if (strlen == 0) {
         return "";
      }
      if (strlen < 0) {
         return "";
      }
      byte[] buf = new byte[strlen];
      in.read(buf);
      return new String(buf, _charEncoding);
   }
   // primitive serialization

   public static void writeToStream(double data,  DataOutputStream out) throws IOException { out.writeDouble(data);}
   public static void writeToStream(float data,   DataOutputStream out) throws IOException { out.writeFloat(data);}
   public static void writeToStream(long data,    DataOutputStream out) throws IOException { out.writeLong(data);}
   public static void writeToStream(int data,     DataOutputStream out) throws IOException { out.writeInt(data);}
   public static void writeToStream(short data,   DataOutputStream out) throws IOException { out.writeShort(data);}
   public static void writeToStream(byte data,    DataOutputStream out) throws IOException { out.writeByte(data);}
   public static void writeToStream(boolean data, DataOutputStream out) throws IOException { out.writeBoolean(data);}
   public static void writeToStream(Integer data, DataOutputStream out) throws IOException { out.writeInt(data.intValue());}
   public static void writeToStream(Byte data,    DataOutputStream out) throws IOException { out.writeByte(data.byteValue());}

   public static double  readDouble (DataInputStream in) throws IOException { return in.readDouble();}
   public static float   readFloat  (DataInputStream in) throws IOException { return in.readFloat();}
   public static long    readLong   (DataInputStream in) throws IOException { return in.readLong();}
   public static int     readInt    (DataInputStream in) throws IOException { return in.readInt();}
   public static short   readShort  (DataInputStream in) throws IOException { return in.readShort();}
   public static byte    readByte   (DataInputStream in) throws IOException { return in.readByte();}
   public static boolean readBoolean(DataInputStream in) throws IOException { return in.readBoolean();}
   public static Integer readInteger(DataInputStream in) throws IOException { return Integer.valueOf(in.readInt());}

   // List serialization (can contain Strings, Integer or SerializableObjects objects)
   public static void writeToStream(List<? extends Object> data, DataOutputStream out) throws IOException {
      if (data == null) {
         out.writeShort(0);
      }
      else {
         out.writeShort(data.size());
         for (int i = 0; i < data.size(); i++) {
            Object obj = data.get(i);
            if (obj instanceof SerializableObject) {
               SerializableObject object = (SerializableObject) obj;
               String key = SerializableFactory.getKey(object);
               try {
                  writeToStream(key, out);
               } catch (IOException e) {
                  e.printStackTrace();
               }
               object.serializeToStream(out);
            }
            else {
               writeObject(obj, out);
            }
         }
      }
   }

   public static int readIntoListString(List<String> data, DataInputStream in) throws IOException {
      data.clear();
      int size = in.readShort();
      for (int i=0 ; i<size ; i++) {
         data.add(readString(in));
      }
      return size;
   }

   public static int readIntoListInteger(List<Integer> data, DataInputStream in) throws IOException {
      data.clear();
      int size = in.readShort();
      for (int i=0 ; i<size ; i++) {
         data.add(Integer.valueOf(in.readInt()));
      }
      return size;
   }

   public static int readIntoListByte(List<Byte> data, DataInputStream in) throws IOException {
      data.clear();
      int size = in.readShort();
      for (int i=0 ; i<size ; i++) {
         data.add(Byte.valueOf(in.readByte()));
      }
      return size;
   }

   public static int readIntoListBoolean(List<Boolean> data, DataInputStream in) throws IOException {
      data.clear();
      int size = in.readShort();
      for (int i=0 ; i<size ; i++) {
         data.add(Boolean.valueOf(in.readBoolean()));
      }
      return size;
   }

   // int[] serialization
   public static void writeToStream(byte[] data, DataOutputStream out) throws IOException {
      out.writeByte(data.length);
      for (byte i = 0; i < data.length; i++) {
         out.writeByte(data[i]);
      }
   }

   public static byte[] readByteArray(DataInputStream in) throws IOException {
      byte[] array = new byte[in.readByte()];
      for (int i=0 ; i<array.length ; i++) {
         array[i] = in.readByte();
      }
      return array;
   }

   public static ArrayList<SerializableObject> readIntoListSerializableObject(DataInputStream in) throws IOException {
      ArrayList<SerializableObject> data = new ArrayList<>();
      int size = in.readShort();
      for (int i=0 ; i<size ; i++) {
         try {
            String key = readString(in);
            SerializableObject object = SerializableFactory.readObject(key, in);
            data.add(object);
         } catch (IOException ex) {
            ex.printStackTrace();
         }
      }
      return data;
   }

}
