///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package automenta.netention;
//
//import com.google.common.base.Predicate;
//
//import java.util.List;
//import java.util.Set;
//
///**
// * Tag = data class
// * @author me
// */
//public class NTag extends NObject.HashNObject {
//
//    static NTag asNObject(final String sysTag) {
//        //TODO cache
//        return new NTag(sysTag, sysTag);
//    }
//
//
//    protected NTag(String id) {
//        this(id, id);
//    }
//
//    protected NTag(String id, String name) {
//        this(id, name, (String)null);
//    }
//
//    public NTag(String id, String name, String extend) {
//        super(id, name);
//
//
//        addDefaultTags();
//        if (extend!=null)
//            put(extend, 1.0);
//
//    }
//
//    protected void addDefaultTags() {
//        put(Tag.tag, 1.0);
//    }
//
//
//
//    public NTag(String id, String name, List<String> extend) {
//        this(id, name, (String)null);
//
//        for (String c : extend) {
//            c = c.trim();
//            if (c.length() == 0) continue;
//            put(c, 1.0);
//        }
//    }
//
//    public void mergeFrom(NTag c) {
//        //TODO
//    }
//
//    public Set<String> getSuperTags() {
//        return getTags(new Predicate<String>() {
//            @Override public boolean apply(String t) {
//                return (!t.equals(Tag.tag));
//            }
//        });
//    }
//
//
//}
