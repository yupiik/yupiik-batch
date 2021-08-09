const bodyParser = require('body-parser');
const { JSONRPCServer } = require('json-rpc-2.0');

// mocks data
const execution = require('../mocks/execution.json');
const executions = require('../mocks/executions.json');
const lastExecutions = require('../mocks/last-executions.json');

// register mocks (for now we don't handle pagination but it is fine to work on views)
const server = new JSONRPCServer();
server.addMethod('yupiik-batch-execution', () => execution);
server.addMethod('yupiik-batch-executions', () => executions);
server.addMethod('yupiik-batch-last-executions', () => lastExecutions);

// bind the jsonrpc endpoint
module.exports = function (app) {
    app.use(bodyParser.json());
    app.post('/jsonrpc', (req, res) => {
        const request = req.body;
        if (Array.isArray(request)) {
            Promise.all(request.map(it => server.receive(it)))
                .then((responses) => res.json(responses))
        } else {
            server
                .receive(request)
                .then((jsonRPCResponse) => res.json(jsonRPCResponse));
        }
    });
};
