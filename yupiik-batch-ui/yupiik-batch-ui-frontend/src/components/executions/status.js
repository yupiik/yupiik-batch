export function getStatusColor(status) {
    switch (status) {
        case 'SUCCESS':
            return 'green';
        case 'FAILURE':
            return 'red';
        default:
            return 'grey';
    }
}
