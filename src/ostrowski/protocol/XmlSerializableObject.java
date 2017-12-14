package ostrowski.protocol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

public abstract class XmlSerializableObject
{

   public abstract Element getXmlObject(Document mapDoc, ArrayList<Integer> includeKnownByUniqueIDInfo, String string);

   public boolean serializeToFile(File destFile)
   {
      Document doc = getXmlObject(null/*includeKnownByUniqueIDInfo*/);

      try (FileOutputStream fos = new FileOutputStream(destFile)) {
         DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
         DOMImplementationLS impl = (DOMImplementationLS) reg.getDOMImplementation("LS");
         LSSerializer serializer = impl.createLSSerializer();
         LSOutput lso = impl.createLSOutput();
         lso.setByteStream(fos);
         serializer.write(doc, lso);
      } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex) {
         ex.printStackTrace();
      }
      return false;
   }

   public Document getXmlObject(ArrayList<Integer> includeKnownByUniqueIDInfo) {
      // Create a builder factory
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(true/*validating*/);

      // Create the builder and parse the file
      Document mapDoc = null;
      try {
         DocumentBuilder builder = factory.newDocumentBuilder();
         mapDoc = builder.newDocument();
         Element element = getXmlObject(mapDoc, includeKnownByUniqueIDInfo, "\n");
         mapDoc.appendChild(element);
      } catch (ParserConfigurationException ex) {
         ex.printStackTrace();
      }
      return mapDoc;
   }

}
