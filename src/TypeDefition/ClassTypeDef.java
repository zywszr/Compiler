package TypeDefition;

import java.util.*;

public class ClassTypeDef extends TypeDef {
    Map <String, TypeDef> objects;

    public ClassTypeDef() {
        objects = new HashMap<>();
    }

    public ClassTypeDef(Map <String, TypeDef> objectList) {
        objects = new HashMap<>();
        for (Map.Entry <String, TypeDef> iter : objectList.entrySet()) {
            objects.put(iter.getKey(), iter.getValue());
        }
    }

    public void insertObject(String key, TypeDef typ) {
        objects.put(key, typ);
    }

    @Override public boolean equals(TypeDef other) {
        if (!(other instanceof ClassTypeDef)) return false;
        if (objects.size() != ((ClassTypeDef)other).objects.size()) return false;
        for (String p : objects.keySet()) {
            if (!((ClassTypeDef)other).objects.containsKey(p) ||
                !((ClassTypeDef)other).objects.get(p).equals(objects.get(p))){
                return false;
            }
        }
        return true;
    }

    public boolean contain(String str) {
        return objects.containsKey(str);
    }

    public TypeDef getEntity(String str) {
        return objects.get(str);
    }
}
