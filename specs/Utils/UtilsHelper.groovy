package Utils

class UtilsHelper {
    static String randomize(String str = ""){
        String randomStr = UUID.randomUUID();

        return str.isEmpty() ? randomStr : str + "_" + randomStr;
    }
}