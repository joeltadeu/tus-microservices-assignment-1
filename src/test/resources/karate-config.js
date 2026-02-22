function fn() {

    var port = karate.properties['karate.port'];
    var config = {
        baseUrl: 'http://localhost:' + port
    };

    karate.configure('connectTimeout', 5000);
    karate.configure('readTimeout', 10000);
    karate.configure('logPrettyRequest', true);
    karate.configure('logPrettyResponse', true);

    return config;
}