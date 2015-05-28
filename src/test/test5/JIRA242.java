package test5;

public class JIRA242 {
    static interface IBooleanSeries {
        public void setValue(boolean value);
    }

    public static class BooleanDataSeries implements IBooleanSeries{
        @Override
        public void setValue(boolean value) {}
    }

    public static class Hello {
        IBooleanSeries BOOL_SERIES;

        public int say() {
            System.out.println("Hello end :) ");
            return 0;
        }

        public IBooleanSeries createBooleanSeriesStep() {
            return new BooleanDataSeries();
        }
    }
}
