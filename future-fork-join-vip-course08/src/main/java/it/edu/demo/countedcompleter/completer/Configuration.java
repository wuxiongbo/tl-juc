package it.edu.demo.countedcompleter.completer;

public class Configuration {

    private boolean skipIncrement;

    private boolean skipCompleteCall;

    private boolean skipTryCompleteCall;

    public Configuration(boolean skipIncrement,
                         boolean skipCompleteCall,
                         boolean skipTryCompleteCall) {
        this.skipIncrement = skipIncrement;
        this.skipCompleteCall = skipCompleteCall;
        this.skipTryCompleteCall = skipTryCompleteCall;
    }

    public boolean canSkipIncrement() {
        return skipIncrement;
    }

    public boolean canSkipCompleteCall() {
        return skipCompleteCall;
    }

    public boolean canSkipTryCompleteCall() {
        return skipTryCompleteCall;
    }

}
