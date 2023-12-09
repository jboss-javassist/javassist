package javassist;

class Animal {
}

class Bear extends Animal {
}


/**
 * Base class has a method with precise type.
 */
class Man {
    String feed(Bear bear) {
        return "Man feed(Bear)";
    }
}

/**
 * Derived class has a method which has same name with base class's and more imprecise type.
 */
class Keeper extends Man {
    String feed(Animal animal) {
        return "Keeper feed(Animal)";
    }
}

/**
 * Derived class has a method which call super method with precise type.
 */
class BearKeeper extends Keeper {
    public BearKeeper() {
    }

    String javacResult() {
        return super.feed(new Bear());
    }
}
