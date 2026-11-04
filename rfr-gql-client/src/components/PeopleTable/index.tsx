import {
    Avatar,
    Box,
    Card,
    Divider,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableRow,
    Typography,
    useMediaQuery,
    useTheme
} from "@mui/material";
import {HeadCell} from "../Table/HeadCell";
import {TableHead} from "../Table/TableHead";
import {TablePagination} from "../Table/Pagination";
import {User} from "../../types/User";
import {FC} from "react";
import {ActionButtons} from "../Table/ActionButtons";
import PeopleOutlineOutlinedIcon from '@mui/icons-material/PeopleOutlineOutlined';
import {TableToolbar} from "../Table/TableToolbar";

const headCells: readonly HeadCell[] = [
    {
        id: 'avatar',
        numeric: false,
        label: 'Avatar',
    },
    {
        id: 'username',
        numeric: false,
        label: 'Username',
    },
    {
        id: 'firstname',
        numeric: false,
        label: 'Name',
    },
    {
        id: 'surname',
        numeric: false,
        label: 'Surname',
    },
    {
        id: 'country',
        numeric: false,
        label: 'Location',
    },
    {
        id: 'actions',
        numeric: true,
        label: 'Actions',
    },
];

interface PeopleTableInterface {
    data: User[];
    page: number;
    hasPreviousPage: boolean;
    hasNextPage: boolean;
    setPage: (page: number) => void;
    setSearch: (value: string) => void;
    onSearchSubmit: () => void;
}

const Location: FC<{ user: User }> = ({user}) => (
    <Box component="span" sx={{display: "inline-flex", alignItems: "center", gap: 1}}>
        <img width={20} src={user.location?.flag ?? ""} alt=""/>
        {user.location?.name}
    </Box>
);

const fullName = (user: User) => [user.firstname, user.surname].filter(Boolean).join(" ");

export const PeopleTable: FC<PeopleTableInterface> = ({
                                                          data,
                                                          page,
                                                          hasPreviousPage,
                                                          hasNextPage,
                                                          setPage,
                                                          setSearch,
                                                          onSearchSubmit
                                                      }) => {
    const theme = useTheme();
    const compact = useMediaQuery(theme.breakpoints.down("md"));

    return (
        <Card>
            <TableToolbar setSearch={setSearch} onSearchSubmit={onSearchSubmit}/>
            {data?.length > 0 && (compact ? (
                <Stack divider={<Divider/>} sx={{borderTop: 1, borderColor: "divider"}}>
                    {data.map((row: User) => (
                        <Box key={row.id} data-test-id="people-row" sx={{display: "flex", alignItems: "center", gap: 2, px: 2, py: 1.5, flexWrap: "wrap"}}>
                            <Avatar src={row.avatar} sx={{width: 44, height: 44}}/>
                            <Box sx={{flex: 1, minWidth: 140}}>
                                <Typography sx={{fontWeight: 600}}>{row.username}</Typography>
                                <Typography variant="body2" sx={{color: "text.secondary"}}>
                                    {fullName(row) || "---"}
                                </Typography>
                                <Typography variant="body2" sx={{color: "text.secondary", mt: 0.25}}>
                                    <Location user={row}/>
                                </Typography>
                            </Box>
                            <ActionButtons userId={row.id} friendStatus={row.friendStatus}/>
                        </Box>
                    ))}
                </Stack>
            ) : (
                <TableContainer>
                    <Table aria-labelledby="tableTitle">
                        <TableHead headCells={headCells}/>
                        <TableBody>
                            {data.map((row: User) => (
                                    <TableRow key={row.id} hover tabIndex={-1}>
                                        <TableCell component="th" scope="row" sx={{py: 1}}>
                                            <Avatar src={row.avatar}/>
                                        </TableCell>
                                        <TableCell sx={{fontWeight: 600}}>{row.username}</TableCell>
                                        <TableCell>{row.firstname ?? "---"}</TableCell>
                                        <TableCell>{row.surname ?? "---"}</TableCell>
                                        <TableCell><Location user={row}/></TableCell>
                                        <TableCell align="right">
                                            <ActionButtons userId={row.id} friendStatus={row.friendStatus}/>
                                        </TableCell>
                                    </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            ))}
            {!data?.length && (
                <Box sx={{textAlign: "center", py: 10, px: 3, borderTop: 1, borderColor: "divider"}}>
                    <PeopleOutlineOutlinedIcon sx={{fontSize: 72, color: "primary.main", opacity: 0.6}}/>
                    <Typography variant="h6" component="p" sx={{mt: 2}}>
                        There are no users yet
                    </Typography>
                </Box>
            )}
            <TablePagination
                onPreviousClick={() => setPage(page - 1)}
                onNextClick={() => setPage(page + 1)}
                hasPreviousValues={hasPreviousPage}
                hasNextValues={hasNextPage}
            />
        </Card>
    )
}
