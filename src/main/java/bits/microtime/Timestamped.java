//package bits.microtime;
//
//import java.util.Comparator;
//
///**
// * @author Philip DeCamp
// */
//public interface Timestamped {
//
//    public static final Comparator<Timestamped> TIME_ORDER = new Comparator<Timestamped>() {
//        public int compare(Timestamped t1, Timestamped t2) {
//            long v1 = t1.getTimestampMicros();
//            long v2 = t2.getTimestampMicros();
//
//            if(v1 < v2)
//                return -1;
//
//            if(v1 > v2)
//                return 1;
//
//            return 0;
//        }
//    };
//
//    public long getTimestampMicros();
//
//}
