package testproxy;

public class Target127 {
    public interface Item { }
    public interface CovariantItem extends Item { }

    public interface Super {
        Item item();
    }

    public interface Sub extends Super {
        CovariantItem item();
    }

    public static class RealSub implements Sub {
        public CovariantItem item() {
            return null;
        }
    }
}
