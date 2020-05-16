package pl.zlooo.fixyou.model;

public enum ApplicationVersionID {

    FIX27(new char[]{'0'}), FIX30(new char[]{'1'}), FIX40(new char[]{'2'}), FIX41(new char[]{'3'}), FIX42(new char[]{'4'}), FIX43(new char[]{'5'}), FIX44(new char[]{'6'}),
    FIX50(new char[]{'7'}), FIX50SP1(new char[]{'8'}), FIX50SP2(new char[]{'9'});

    private final char[] value;

    ApplicationVersionID(char[] value) {
        this.value = value;
    }

    public char[] getValue() {
        return value;
    }
}
