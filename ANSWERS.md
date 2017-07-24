# Question 1

This implementation with an HTTP reverse proxy works for the given stream because the playlist contains only relative URLs and there is only one remote host. If the player followed an absolute URL, requests would stop going through the reverse proxy. 

Moreover, the current implementation prevents HTTP pipelining, which might reduce performance if the player implements it.

# Question 2

Like it is described in the answer to the previous question, the app is not compatible with streams featuring absolute URLs, in that case, the requests would not be intercepted properly.

Also, the current app does not have any fallback mode (for instance redirecting the player to a non-intercepted stream), so any issue with the HTTP forwarding process might lead to an unreacheable video. There are no analytics or error reporting mechanisms, so if the application was deployed in production, nothing would be reported in case of problems.

# Bonus question

The app should select a free TCP port if the default one is busy.

To be compatible with all streams, the playlist files could be parsed and interpreted to replace absolute URLs. The HTTP forwarder could be extended to forward to multiple hosts, for instance using the first directory of the path as a hint.

The HTTP forwarder could be refactored to support HTTP pipelining. A better architecture would be to read HTTP requests as soon they come from a single connection, forward them, and store them in a queue for a separate thread which would forward back the responses on the connection without closing it.
