export default function reducer(state, action) {
    switch (action.type) {
        default:
            return {
                ...state,
                [action.type]: action.value,
            };
    }
};
