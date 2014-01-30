package me.tatarka.ipromise;

public final class Folds {
    private Folds() {}

    public static Fold<Integer, Integer> sumInt() {
        return new Fold<Integer, Integer>() {
            @Override
            public Integer fold(Integer accumulator, Integer item) {
                return accumulator + item;
            }
        };
    }

    public static Fold<Float, Float> sumFloat() {
        return new Fold<Float, Float>() {
            @Override
            public Float fold(Float accumulator, Float item) {
                return accumulator + item;
            }
        };
    }
}
