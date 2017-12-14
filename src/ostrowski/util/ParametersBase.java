package ostrowski.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public abstract class ParametersBase {

   private final String propertiesFilename;
   private final URL context;

   protected Properties properties = null;

   public ParametersBase(String propertiesFilename) {
      this.propertiesFilename = propertiesFilename;
      context = null;
      getParameters();
   }

   protected ParametersBase(URL context, String propertiesFilename) {
      this.propertiesFilename = propertiesFilename;
      this.context = context;
      getParameters();
   }

   abstract protected void setDefaults(Properties defaults) ;
   abstract protected void updateVarsFromFileSettings() ;
   abstract protected void updateFileSettingsFromVars() ;

   public boolean getParameters() {
      System.out.println("Attempting to load properties from property file: " + propertiesFilename);

      Properties defaults = new Properties();
      boolean success = true;

      setDefaults(defaults);

      properties = new Properties(defaults);

      if (context != null) {
         try {
            URL fileURL = new URL(context, propertiesFilename);
            properties.load(fileURL.openStream());
         }
         catch (MalformedURLException ex) {
            success = false;
            System.err.println("ParametersBase: getParameters: Malformed URL Exception While Opening " +
               propertiesFilename + ". Using defaults.");

            ex.printStackTrace();
         }
         catch (java.io.IOException e) {
             success = false;
             System.err.println("ParametersBase: getParameters: IOException While Opening " +
                  propertiesFilename + ". Using defaults.");

             e.printStackTrace();
         }
      }
      else {
         try (FileInputStream in = new FileInputStream(propertiesFilename)) {
            properties.load(in);
            System.out.println("Successfully loaded properties from property file: " + propertiesFilename);
         }
         catch (java.io.FileNotFoundException ex) {
            success = false;
            System.out.println("Failed to locate property file: " +
               propertiesFilename + " - Using defaults.");
         }
         catch (java.io.IOException ex) {
            success = false;
            System.err.println("ParametersBase: getParameters: Can't read property file: " +
                                propertiesFilename + ".  Using defaults.");

            ex.printStackTrace();
         }
      }

      updateVarsFromFileSettings();
      return success;
   }

   public void setParameters() {
      updateFileSettingsFromVars();
      try (FileOutputStream out = new FileOutputStream(propertiesFilename)) {
         properties.store(out, "");
      } catch (java.io.IOException ex) {
         System.err.println("ParametersBase: setParameters: IOException trying to save property file " +
            propertiesFilename);
         ex.printStackTrace();
      }
   }
}
