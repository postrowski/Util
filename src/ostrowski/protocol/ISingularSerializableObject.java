package ostrowski.protocol;

public interface ISingularSerializableObject
{
   String getMapKey();

   void plantInitialObject();

   ISingularSerializableObject findExistingObject();

   void copyDataFrom(SerializableObject newSerObj);
}
