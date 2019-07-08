package util.log;


/**
 * @Author : Rookiex
 * @Date : 2019/07/05
 * @Describe :
 */
public class LogFactory {

    private static Logger log = new Logger();
    public static Logger getLogger(Class<?> aClass) {
        return log;
    }
}
