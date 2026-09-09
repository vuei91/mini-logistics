function handler(event) {
    var request = event.request;
    var uri = request.uri;

    // 1) API 요청은 절대 건드리지 않는다(백엔드 EC2 오리진으로 그대로 전달).
    if (uri.startsWith('/api/')) {
        return request;
    }

    // 2) 정적 파일 요청(.js, .css, .png, .ico 등)은 그대로 둔다.
    if (uri.includes('.')) {
        return request;
    }

    // 3) 폴더형 경로(trailingSlash: true 로 export 됨)를 index.html 로 매핑.
    //    쿼리스트링(?id=) 방식이라 동적 세그먼트가 없으므로 특수 처리가 필요 없다.
    //    /                          -> /index.html
    //    /login                     -> /login/index.html
    //    /shipper/requests/detail   -> /shipper/requests/detail/index.html  (?id= 는 쿼리라 uri 에 없음)
    if (uri.endsWith('/')) {
        request.uri = uri + 'index.html';
    } else {
        request.uri = uri + '/index.html';
    }

    return request;
}
