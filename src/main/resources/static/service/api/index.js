const METHOD = {
    GET(token) {
        const headers = setHeadersIfTokenExist({}, token)
        return {
            method: 'GET',
            headers: headers
        }
    },
    PUT(data, token) {
        const headers = setHeadersIfTokenExist({'Content-Type': 'application/json'}, token)
        return {
            method: 'PUT',
            headers: headers,
            body: JSON.stringify({
                ...data
            })
        }
    },
    DELETE(token) {
        const headers = setHeadersIfTokenExist({}, token)
        return {
            method: 'DELETE',
            headers: headers
        }
    },
    POST(data, token) {
        const headers = setHeadersIfTokenExist({'Content-Type': 'application/json'}, token)
        return {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({
                ...data
            })
        }
    },
}

const setHeadersIfTokenExist = (headers, token) => {
    if (token) {
        return {
            ...headers,
            authorization: token
        }
    }
    return headers
}

const api = (() => {
    const request = (uri, config) => fetch(uri, config).then(data => {
        if (!data.ok) {
            return data.json().then(error => {
                throw new Error(error.errorMessage)
            })
        }
    })
    const requestWithJsonData = (uri, config) => fetch(uri, config).then(data => {
        if (data.ok) {
            return data.json()
        }
        return data.json().then(error => {
            throw new Error(error.errorMessage)
        })
    })

    const line = {
        getAll() {
            return request(`/lines/detail`)
        },
        getAllDetail() {
            return requestWithJsonData(`/lines/detail`)
        }
    }

    const path = {
        find(params) {
            return requestWithJsonData(`/paths?source=${params.source}&target=${params.target}&type=${params.type}`)
        }
    }

    const member = {
        join(params) {
            return request(`/members`, METHOD.POST(params))
        },
        login(params) {
            return requestWithJsonData(`/oauth/token`, METHOD.POST(params))
        }
    }

    const getToken = () => sessionStorage.getItem('token')

    const me = {
        retrieve() {
            return requestWithJsonData(`/me`, METHOD.GET(getToken()))
        },
        update(data) {
            return request(`/me`, METHOD.PUT(data, getToken()))
        }
    }

    return {
        line,
        path,
        member,
        me
    }
})()

export default api
